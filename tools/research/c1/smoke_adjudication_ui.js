"use strict";

const fs = require("fs");

class Element {
  constructor(tag = "div") {
    this.tag = tag;
    this.children = [];
    this.options = [];
    this.events = {};
    this.style = {};
    this.attributes = {};
    this.disabled = false;
    this.hidden = false;
    this.value = "";
    this.text = "";
  }
  set textContent(value) { this.text = String(value); this.children = []; }
  get textContent() { return this.text; }
  append(...items) {
    this.children.push(...items);
    if (this.tag === "select") this.options.push(...items);
  }
  replaceChildren(...items) { this.children = []; this.options = []; this.append(...items); }
  addEventListener(name, callback) { this.events[name] = callback; }
  setAttribute(name, value) { this.attributes[name] = value; }
  click() { if (this.onclick) this.onclick({}); }
  remove() {}
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function run(htmlPath) {
  const html = fs.readFileSync(htmlPath, "utf8");
  const packetText = html.match(/<script id="packet" type="application\/json">([\s\S]*?)<\/script>/)?.[1];
  const code = html.match(/<script>([\s\S]*?)<\/script>/)?.[1];
  assert(packetText && code, "standalone HTML must contain packet and application script");
  assert(!/<(?:script|link)[^>]+(?:src|href)=["']https?:/i.test(html), "standalone form must not load external resources");

  const elements = new Map();
  const storage = new Map();
  const downloads = [];
  const body = new Element("body");
  global.document = {
    body,
    getElementById(id) {
      if (id === "packet") return { textContent: packetText };
      if (!elements.has(id)) elements.set(id, new Element(id === "jump" ? "select" : "div"));
      return elements.get(id);
    },
    createElement(tag) { return new Element(tag); },
  };
  global.localStorage = {
    getItem(key) { return storage.get(key) || null; },
    setItem(key, value) { storage.set(key, value); },
  };
  global.URL = {
    createObjectURL(blob) { downloads.push(blob.parts.join("")); return "blob:local"; },
    revokeObjectURL() {},
  };
  global.Blob = class { constructor(parts) { this.parts = parts; } };
  global.setTimeout = () => {};
  global.window = { scrollTo() {} };
  global.confirm = () => true;

  new Function(code)();
  function findInputs(root, found = []) {
    if (!root || typeof root !== "object") return found;
    if (root.tag === "input" && root.name === "final-label") found.push(root);
    for (const child of root.children || []) findInputs(child, found);
    return found;
  }

  const choices = findInputs(elements.get("content"));
  assert(choices.length === 4, "all four final-label choices must render");
  assert(elements.get("content").children.some((child) => child.tag === "fieldset"), "label controls must be attached to the visible form");
  choices[0].events.change();
  assert(elements.get("progressText").textContent.startsWith("1 of 150"), "progress must preserve the fixed denominator");
  assert(storage.size === 1, "answer must be persisted locally");

  elements.get("download").onclick();
  const returned = JSON.parse(downloads[0]);
  assert(returned.expected_items === 150 && returned.decisions.length === 1 && returned.complete === false, "progress export must preserve denominator and partial state");
  assert(Object.keys(returned.decisions[0]).sort().join(",") === "final_label,item_id,rationale", "export must contain only opaque ID and adjudication fields");
  assert(/^[a-f0-9]{24}$/.test(returned.decisions[0].item_id), "exported join ID must be opaque");
  assert(!Object.keys(returned).some((key) => /case|rater|gold|arm|rank/i.test(key)), "export must not expose private metadata");

  elements.get("next").onclick();
  assert(elements.get("position").textContent.startsWith("Item 2 / 150"), "navigation must advance one item");
  const fileInput = elements.get("file");
  fileInput.files = [{ size: downloads[0].length, text: async () => downloads[0] }];
  return fileInput.onchange({ target: fileInput }).then(() => {
    assert(elements.get("progressText").textContent.startsWith("1 of 150"), "export must restore saved decisions");
    const invalid = { ...returned, packet_sha256: "0".repeat(64) };
    fileInput.files = [{ size: 3000, text: async () => JSON.stringify(invalid) }];
    return fileInput.onchange({ target: fileInput }).then(() => {
      assert(elements.get("notice").hidden === false, "foreign/tampered return must be rejected visibly");
      process.stdout.write("PASS: four choices, fixed denominator, local save, export, navigation, restore, tamper rejection\n");
    });
  });
}

const htmlPath = process.argv[2];
if (!htmlPath) {
  process.stderr.write("Usage: node smoke_adjudication_ui.js <private-review.html>\n");
  process.exit(2);
}
run(htmlPath).catch((error) => {
  process.stderr.write(`${error.message}\n`);
  process.exit(1);
});
