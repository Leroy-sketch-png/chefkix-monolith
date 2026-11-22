#!/usr/bin/env node
// Build a standalone review copy from the pinned Lead handoff in a Git revision.
const { execFileSync } = require('node:child_process')
const { createHash } = require('node:crypto')
const { mkdirSync, writeFileSync } = require('node:fs')
const { dirname, resolve, posix } = require('node:path')
const { Marked } = require('marked')
const katex = require('katex')

const root = resolve(__dirname, '../../..')
const handoff = 'evidence/iron-chef-lead-member-handoff-20261105'
const exhibit = 'evidence/member-l36-thesis-exhibit-20261122'
const output = process.argv[2]
if (!output) throw new Error('Usage: node build_manuscript.cjs OUTPUT.html [SOURCE_REVISION]')

const git = (...args) => execFileSync('git', ['-C', root, ...args], { maxBuffer: 24 * 1024 * 1024 })
const revision = git('rev-parse', '--verify', '--end-of-options', (process.argv[3] || 'HEAD') + '^{commit}').toString().trim()
const tracked = new Set(git('ls-tree', '--full-tree', '-r', '--name-only', '-z', revision).toString().split('\0'))
const pinned = path => git('show', revision + ':' + path)
const manifest = JSON.parse(pinned(handoff + '/SOURCE.json'))
const manifestChapters = manifest.direct_authored_copies.filter(entry => /^chapters\/\d{2}-.+\.md$/.test(entry.destination_path))
const digest = bytes => createHash('sha256').update(bytes).digest('hex')
const escape = text => String(text).replace(/[&<>"']/g, char => ({
  '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
})[char])
const slug = text => text.toLowerCase().trim().replace(/[^\p{L}\p{N} _-]/gu, '').replace(/\s+/g, '-')
const fromRepo = (path, fragment = '') => 'https://github.com/' + manifest.destination_repository_slug +
  '/blob/' + revision + '/' + path.split('/').map(encodeURIComponent).join('/') + fragment
if (!/^[\w.-]+\/[\w.-]+$/.test(manifest.destination_repository_slug)) throw new Error('Invalid manifest repository slug')

const chapters = manifestChapters.map(entry => {
  const path = handoff + '/' + entry.destination_path
  const number = posix.basename(path).slice(0, 2)
  const bytes = pinned(path)
  if (digest(bytes) !== entry.destination_sha256.toLowerCase()) throw new Error('Chapter hash mismatch: ' + path)
  const markdown = bytes.toString('utf8')
  const title = markdown.match(/^# (.+)\r?$/m)?.[1].trim()
  if (!title?.startsWith('Chapter ' + Number(number) + ' ')) throw new Error('Unexpected heading: ' + path)
  return { id: 'chapter-' + number, number, path, sourcePath: entry.source_path, markdown, title }
}).sort((a, b) => a.number.localeCompare(b.number))
if (chapters.length !== 14 || chapters.some((chapter, index) => Number(chapter.number) !== index + 1)) {
  throw new Error('The handoff must provide exactly Chapters 1 through 14')
}
const chapterByPath = new Map(chapters.map(chapter => [chapter.path, chapter]))
const sourceCopies = new Map()
for (const group of ['included_files', 'direct_authored_copies', 'authored_evidence_copies', 'lead_authored_addenda', 'lead_authored_contracts']) {
  for (const entry of manifest[group] || []) {
    if (entry.source_path && entry.destination_path && (entry.destination_sha256 || entry.sha256)) {
      sourceCopies.set(entry.source_path, entry)
    }
  }
}
const counts = { equations: 0, tables: 0, chapterLinks: 0, sharedSourceLinks: 0, externalLinks: 0, unavailableLinks: 0 }
const unavailable = new Map()

const math = {
  name: 'manuscriptMath',
  level: 'inline',
  start(src) {
    return src.indexOf('$')
  },
  tokenizer(src) {
    const match = /^\$(?![\s$])((?:\\.|[^$\r\n])+?)\$(?![$\p{L}\p{N}])/u.exec(src)
    if (match) return { type: 'manuscriptMath', raw: match[0], tex: match[1] }
  },
  renderer(token) {
    counts.equations += 1
    return '<span class="equation">' +
      katex.renderToString(token.tex, { output: 'mathml', trust: false, throwOnError: true }) + '</span>'
  },
}

function renderChapter(chapter) {
  const parser = new Marked()
  const headingIds = new Map()
  parser.use({ extensions: [math] })
  // Know each anchor before rendering links, including forward links.
  const tokens = parser.lexer(chapter.markdown)
  parser.walkTokens(tokens, token => {
    if (token.type === 'heading') {
      const anchor = slug(token.text)
      const repeats = headingIds.get(anchor) || 0
      const unique = anchor + (repeats ? '-' + repeats : '')
      headingIds.set(anchor, repeats + 1)
      token.documentId = chapter.id + '-' + unique
    }
  })
  chapter.anchors = new Set()
  parser.walkTokens(tokens, token => {
    if (token.type === 'heading') chapter.anchors.add(token.documentId)
  })
  chapter.render = () => {
    const render = new Marked()
    render.use({
      extensions: [math],
      renderer: {
        heading(token) {
          const level = Math.min(token.depth + 1, 6)
          return '<h' + level + ' id="' + escape(token.documentId) + '">' +
            this.parser.parseInline(token.tokens) + '</h' + level + '>\n'
        },
        link(token) {
          const content = this.parser.parseInline(token.tokens)
          const title = token.title ? ' title="' + escape(token.title) + '"' : ''
          const anchor = target => '<a href="' + escape(target) + '"' + title + '>' + content + '</a>'
          if (/^https?:\/\//i.test(token.href) || /^mailto:/i.test(token.href)) {
            counts.externalLinks += 1
            return anchor(token.href)
          }
          const [rawPath, fragment] = token.href.split('#', 2)
          if (!/^[a-z][a-z0-9+.-]*:|^\/\//i.test(rawPath)) {
            const decoded = decodeURIComponent(rawPath)
            let target = rawPath ? posix.normalize(posix.join(posix.dirname(chapter.path), decoded)) : chapter.path
            if (!tracked.has(target)) {
              const sourcePath = posix.normalize(posix.join(posix.dirname(chapter.sourcePath), decoded))
              const exactCopy = sourceCopies.get(sourcePath)
              if (exactCopy) {
                const sharedPath = handoff + '/' + exactCopy.destination_path
                const sha256 = (exactCopy.destination_sha256 || exactCopy.sha256).toLowerCase()
                if (!tracked.has(sharedPath) || digest(pinned(sharedPath)) !== sha256) {
                  throw new Error('Shared reference does not match the manifest: ' + sharedPath)
                }
                target = sharedPath
              }
            }
            const sharedChapter = chapterByPath.get(target)
            if (sharedChapter) {
              const id = fragment ? sharedChapter.id + '-' + decodeURIComponent(fragment) : sharedChapter.id
              if (!fragment || sharedChapter.anchors.has(id)) {
                counts.chapterLinks += 1
                return anchor('#' + id)
              }
            } else if (tracked.has(target)) {
              counts.sharedSourceLinks += 1
              return anchor(fromRepo(target, fragment ? '#' + encodeURIComponent(decodeURIComponent(fragment)) : ''))
            }
          }
          counts.unavailableLinks += 1
          const key = chapter.id + '\t' + token.href
          if (!unavailable.has(key)) unavailable.set(key, { chapter: chapter.number, label: token.text, source: token.href })
          return '<span class="unavailable-ref">' + content + '</span>'
        },
        html(token) {
          return escape(token.text)
        },
        image(token) {
          throw new Error('Review any new chapter image before including it: ' + chapter.path + ' ' + token.text)
        },
        table(token) {
          counts.tables += 1
          const row = (cells, tag) => '<tr>' + cells.map(cell => '<' + tag +
            (tag === 'th' ? ' scope="col"' : '') + '>' + this.parser.parseInline(cell.tokens) + '</' + tag + '>').join('') + '</tr>'
          return '<div class="table-scroll" role="region" aria-label="Chapter table" tabindex="0"><table><thead>' +
            row(token.header, 'th') + '</thead><tbody>' + token.rows.map(cells => row(cells, 'td')).join('') + '</tbody></table></div>\n'
        },
      },
    })
    return '<article id="' + chapter.id + '" class="chapter">' + render.parser(tokens) + '</article>'
  }
}

chapters.forEach(renderChapter)
const content = chapters.map(chapter => chapter.render()).join('\n')
const nav = chapters.map(chapter => '<li><a href="#' + chapter.id + '">' +
  escape(chapter.title) + '</a></li>').join('\n')
const figure = pinned(exhibit + '/l36-h0-outcomes.svg').toString('base64')
const referencesByChapter = new Map()
for (const reference of unavailable.values()) {
  if (!referencesByChapter.has(reference.chapter)) referencesByChapter.set(reference.chapter, [])
  referencesByChapter.get(reference.chapter).push(reference)
}
const appendix = Array.from(referencesByChapter, ([chapter, references]) => {
  const count = references.length
  const items = references.map(reference => '<li>' + escape(reference.label) +
    '<br><code>' + escape(reference.source) + '</code></li>').join('')
  return '<details><summary>Chapter ' + Number(chapter) + ' · ' + count +
    (count === 1 ? ' reference' : ' references') + '</summary><ul>' + items + '</ul></details>'
}).join('')

const html = '<!doctype html>\n' + [
  '<html lang="en"><head><meta charset="utf-8">',
  '<meta name="viewport" content="width=device-width, initial-scale=1">',
  '<meta http-equiv="Content-Security-Policy" content="default-src \'none\'; style-src \'unsafe-inline\'; img-src data:; base-uri \'none\'; form-action \'none\'">',
  '<title>Iron Chef Research Manuscript</title>',
  '<style>',
  ':root{color-scheme:light;--ink:#1b2830;--muted:#465660;--line:#dce3e7;--accent:#15556c;--paper:#fff;--wash:#f3f6f7}',
  '*{box-sizing:border-box}html{scroll-padding-top:1rem}body{margin:0;background:var(--wash);color:var(--ink);font:16px/1.65 Georgia,serif}',
  'a{color:var(--accent);text-underline-offset:.16em}a:focus-visible,[tabindex]:focus-visible{outline:3px solid #b65e23;outline-offset:3px}',
  '.skip{position:absolute;top:.5rem;left:.5rem;z-index:2;background:white;padding:.35rem .8rem;transform:translateY(-150%)}.skip:focus{transform:none}',
  '.shell{width:min(100%,1340px);margin:0 auto;padding:clamp(1rem,3vw,3rem);display:grid;grid-template-columns:minmax(14rem,18rem) minmax(0,1fr);gap:clamp(1.25rem,3vw,3rem)}',
  'nav{position:sticky;top:1rem;align-self:start;max-height:calc(100vh - 2rem);overflow:auto;padding:1.2rem;background:white;border:1px solid var(--line);font:14px/1.45 system-ui,sans-serif}',
  'nav h2{font-size:.93rem;margin:0 0 .8rem}nav ol{padding:0;margin:0;list-style:none}nav li+li{margin-top:.35rem}nav a{display:block;padding:.24rem;text-decoration:none}nav a:hover{text-decoration:underline}',
  'main{background:var(--paper);border:1px solid var(--line);padding:clamp(1rem,4vw,3.75rem);min-width:0}header p{font:16px/1.55 system-ui,sans-serif;color:var(--muted);margin-bottom:.75rem}',
  'h1,h2,h3,h4,h5,h6{font-family:system-ui,sans-serif;line-height:1.22;text-wrap:balance}h1{font-size:clamp(1.8rem,5vw,3.15rem);margin:0}',
  'h2{font-size:clamp(1.4rem,3vw,2.0rem);margin:0 0 1.15rem}h3{font-size:1.18rem;margin:1.9rem 0 .65rem}h4,h5,h6{margin:1.5rem 0 .6rem}',
  'p,ul,ol,blockquote{overflow-wrap:anywhere}p{margin:.65rem 0 .95rem}li+li{margin-top:.24rem}blockquote{border-left:.2rem solid #8fabb5;padding:.1rem 1rem;margin:1rem 0}',
  '.chapter,.appendix{border-top:1px solid var(--line);padding-top:2rem;margin-top:2rem}.chapter>h2{color:#142f3b}',
  'header .review-context{margin-top:1.1rem;border-left:3px solid #91afbb;padding:.45rem .8rem;background:#f1f6f8;font-size:.9rem}',
  'code,pre{font:0.88em/1.5 ui-monospace,Consolas,monospace;background:#f1f5f6;overflow-wrap:anywhere}code{padding:.1rem .25rem}pre{padding:.85rem;white-space:pre-wrap}',
  'hr{border:none;border-top:1px solid var(--line);margin:1.7rem 0}.table-scroll{overflow-x:auto;margin:1rem 0 1.45rem;max-width:100%}',
  'table{width:100%;min-width:32rem;border-collapse:collapse;font:14px/1.5 system-ui,sans-serif;table-layout:auto}th,td{padding:.58rem .68rem;border:1px solid var(--line);vertical-align:top;overflow-wrap:anywhere}',
  'th{background:#eaf1f3;text-align:left}tbody tr:nth-child(even){background:#f8fafb}.katex{font-size:1.03em}.equation{display:inline-block;max-width:100%;overflow-x:auto;vertical-align:middle}.unavailable-ref{color:var(--muted);text-decoration:underline dotted;text-underline-offset:.2em}',
  'details{border-top:1px solid var(--line);padding:.45rem 0}summary{cursor:pointer;font-family:system-ui,sans-serif}',
  'figure{margin:1.2rem 0}.figure-scroll{overflow-x:auto}figure img{display:block;width:100%;min-width:40rem;height:auto}figcaption,.source{font:14px/1.55 system-ui,sans-serif;color:var(--muted)}',
  '@media(max-width:850px){.shell{display:block}nav{position:static;max-height:15rem;margin-bottom:1rem}main{padding:clamp(1rem,6vw,2.4rem)}}',
  '@media print{body{background:white;font-size:10.5pt}.shell{display:block;padding:0;width:100%}main{border:0;padding:0}nav,.skip{display:none}.chapter,.appendix{break-before:page;border:0;margin:0;padding:0}h2,h3,h4{break-after:avoid-page}table{min-width:0;font-size:8.8pt}figure img{min-width:0}.equation,.figure-scroll{overflow:visible}thead{display:table-header-group}tr,figure{break-inside:avoid-page}.table-scroll{overflow:visible}a{color:inherit}}',
  '</style></head><body><a class="skip" href="#manuscript">Skip to manuscript</a>',
  '<div class="shell"><nav aria-label="Manuscript contents"><h2>Contents</h2><ol>' + nav +
    '<li><a href="#chapter-six-exhibit">Chapter 6 evidence figure</a></li><li><a href="#references">Reference availability</a></li></ol></nav>',
  '<main id="manuscript"><header><h1>Iron Chef Research Manuscript</h1><p>Fourteen chapters for Lead review</p>' +
    '<p class="source">Source revision <a href="' + escape(fromRepo(handoff + '/SOURCE.json')) + '">' + revision.slice(0, 7) + '</a></p>' +
    '<p class="review-context">The chapter headers retain their dated evidence snapshots. See the <a href="' +
    escape(fromRepo('BACKLOG_LEAD.md', '#current-evidence-and-recovery')) + '">Lead backlog</a> for current campaign accounting and open review gates.</p></header>',
  content,
  '<section class="appendix" id="chapter-six-exhibit"><h2>Chapter 6 evidence figure</h2>' +
    '<figure><div class="figure-scroll" role="region" tabindex="0" aria-label="Chapter 6 figure"><img alt="97 observed reference misses. 72 strict-A successes, 22 definite failures, and 3 unknowns. Weighted descriptive bounds are 71.15 and 75.22 percent." src="data:image/svg+xml;base64,' +
    figure + '"></div><figcaption>Weighted descriptive bounds for the 97 observed H = 0 cases. Sampling uncertainty and the primary H1 decision remain open.</figcaption></figure>' +
    '<p class="source"><a href="' + escape(fromRepo(exhibit + '/l36-h0-outcomes.csv')) + '">Aggregate table</a> · <a href="' +
    escape(fromRepo('evidence/l36-conditional-h0-descriptive-point-20261122.md')) + '">Source receipt</a></p></section>',
  '<section class="appendix" id="references"><h2>Reference availability</h2>' +
    '<p>Dotted underlines identify citations whose destinations are outside the shared source snapshot or whose anchors are unavailable. Their citation wording is preserved.</p>' +
    (appendix || '<p>All repository citations resolve within this snapshot.</p>') +
    '<p class="source"><a href="' + escape(fromRepo(handoff + '/README.md')) + '">Lead handoff and source boundaries</a></p></section>',
  '</main></div></body></html>',
].join('\n') + '\n'

mkdirSync(dirname(resolve(output)), { recursive: true })
writeFileSync(output, html, 'utf8')
console.log(JSON.stringify({
  revision, chapters: chapters.length, ...counts, uniqueUnavailableReferences: unavailable.size,
  sha256: digest(Buffer.from(html)), bytes: Buffer.byteLength(html),
}))
