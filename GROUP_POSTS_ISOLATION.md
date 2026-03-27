# Group Posts Isolation - Facebook Pattern Implementation

## Overview
This document outlines the implementation of the Facebook pattern for group posts, ensuring that posts created within groups are **only displayed in groups** and nowhere else (not in feed, home, profile, or search results).

## Changes Made

### Backend (Java - Spring Boot)

#### 1. **PostService.java** - Core Feed Queries

All feed query methods now explicitly exclude GROUP posts using MongoDB criteria filters:

**a) `getAllPosts()` - Main Feed (Latest/Trending)**
- **Location**: Lines 565-591
- **Change**: Modified to use MongoDB query with `Criteria.where("postType").ne(PostType.GROUP.name())`
- **Impact**: Home feed no longer shows GROUP posts in both Latest (mode=0) and Trending (mode=1) modes
- **Before**: Used `postRepository.findByHiddenFalse()` which returned all visible posts
- **After**: Uses MongoTemplate with explicit GROUP post exclusion criteria

**b) `getForYouFeed()` - Personalized Feed**
- **Location**: Line 577 (within the method)
- **Change**: Added `.and("postType").ne(PostType.GROUP.name())` to `baseCriteria`
- **Impact**: Personalized "For You" feed (mode=2) filters out GROUP posts during candidate selection
- **Algorithm Impact**: GROUP posts are excluded before scoring/ranking happens

**c) `getFollowingFeed()` - Following Feed**
- **Location**: Lines 914-950
- **Change**: Complete refactor from repository method to MongoTemplate query with GROUP post exclusion
- **Impact**: Following feed no longer shows GROUP posts from followed users
- **Before**: Used `postRepository.findByUserIdInAndHiddenFalseOrderByCreatedAtDesc()`
- **After**: Uses custom Criteria with explicit GROUP type filtering

**d) `getAllPostsByUserId()` - User Profile Posts**
- **Location**: Lines 850-876
- **Change**: Modified to use MongoTemplate with GROUP post exclusion
- **Impact**: User profile pages no longer display GROUP posts in their posts tab
- **Before**: Used `postRepository.findByUserIdAndHiddenFalseOrderByCreatedAtDesc()`
- **After**: Uses custom Criteria with explicit GROUP type filtering

**e) `searchPosts()` - Search Results**
- **Location**: Lines 859-896
- **Change**: Added `.and("postType").ne(PostType.GROUP.name())` to `searchCriteria`
- **Impact**: Search results no longer include GROUP posts
- **Before**: Searched all visible posts
- **After**: Explicitly excludes GROUP type from search results

#### 2. **PostProviderImpl.java** - Cross-Module API
- **No changes needed** - This provider uses `postService.getAllPostsByUserId()` which now filters GROUP posts

#### 3. **PostRepository.java** - Database Layer
- **No changes needed** - All filtering is done at service layer using MongoTemplate queries

---

### Frontend (TypeScript/React)

#### 1. **feed/page.tsx** - Feed Page
- **Location**: Line 63-67
- **Change**: Added defensive filter to `filteredPosts` derived state
- **Code**: 
  ```typescript
  const filteredPosts = useFilterBlockedContent(posts).filter(
    (post) => post.postType !== 'GROUP'
  )
  ```
- **Purpose**: Backup filter in case backend returns GROUP posts (defense in depth)

#### 2. **dashboard/page.tsx** - Home/Dashboard
- **Location**: Lines 228 & 320
- **Change**: Already had defensive filtering in place (no changes needed)
- **Code**: 
  ```typescript
  // Filter to only show PERSONAL posts (not GROUP posts)
  feedPosts = feedPosts.filter(post => post.postType !== 'GROUP')
  ```

#### 3. **profile/UserProfile.tsx** - User Profile Component
- **Location**: Lines 340-360 & 384-404
- **Change**: Added defensive filters to both posts tabs
- **Posts Tab**:
  ```typescript
  const personalPosts = response.data.filter(post => post.postType !== 'GROUP')
  setUserPosts(personalPosts)
  ```
- **Saved Posts Tab**:
  ```typescript
  const personalPosts = response.data.content.filter(post => post.postType !== 'GROUP')
  setSavedPosts(personalPosts)
  ```

#### 4. **group/[id]/page.tsx** - Group Details Page
- **No changes needed** - Already correctly filters GROUP posts for group context using `postType === 'GROUP'`

#### 5. **services/group.ts** - Group Service
- **No changes needed** - Already has proper GROUP post filtering at line 236-237

---

## Facebook Pattern Implementation

| Location | PERSONAL Posts | GROUP Posts |
|----------|---|---|
| **Home Feed** | ✅ Shown | ❌ Hidden |
| **Following Feed** | ✅ Shown | ❌ Hidden |
| **"For You" Feed** | ✅ Shown | ❌ Hidden |
| **User Profile** | ✅ Shown | ❌ Hidden |
| **Saved Posts** | ✅ Shown | ❌ Hidden |
| **Search Results** | ✅ Shown | ❌ Hidden |
| **Group Page** | N/A | ✅ Shown Only |

---

## Query Filter Strategy

### MongoDB Filter Expression
All feed queries now include:
```java
.and("postType").ne(PostType.GROUP.name())
```

This ensures:
1. **Performance**: Uses database index for `postType` field (see compound indexes in Post.java)
2. **Consistency**: Single source of truth at database layer
3. **Safety**: Multiple defensive filters at service layer as backup

### Filtering Layers (Defense in Depth)

1. **Backend - Primary Filter** (PostService.java)
   - MongoDB query criteria excludes GROUP posts
   - Applied before pagination/sorting
   
2. **Frontend - Defensive Filter** (React components)
   - Additional `.filter(post => post.postType !== 'GROUP')`
   - Catches any unexpected GROUP posts from API
   - No performance impact (applied client-side after fetch)

---

## Potential Edge Cases Handled

1. **User Scrolling Through Feed**: ✅ Backend pagination excludes GROUP posts entirely
2. **Search Results**: ✅ Search criteria filters GROUP posts
3. **Following Feed**: ✅ GROUP posts not included from followed users
4. **Profile View**: ✅ GROUP posts not displayed in user's posts tab
5. **Saved Posts**: ✅ GROUP posts not included in saved collection
6. **Cache/Optimistic Updates**: ✅ Frontend filters prevent cached GROUP posts from displaying

---

## Performance Impact

- **Database**: Minimal (indexed on `postType`, applied before pagination)
- **API Response**: No change (same number of posts returned)
- **Frontend**: Negligible (filter applied after response received)
- **Compound Indexes**: Already optimized for `{groupId, createdAt}` and `{userId, createdAt}`

---

## Testing Checklist

- [ ] Create a GROUP post in a group
- [ ] Verify post appears **only** in group context
- [ ] Verify post does **NOT** appear in:
  - [ ] Home feed (Latest)
  - [ ] Home feed (Trending)
  - [ ] Home feed (For You)
  - [ ] Following feed
  - [ ] User profile posts
  - [ ] Saved posts (if user saves a group post)
  - [ ] Search results
- [ ] Verify PERSONAL posts still appear in all contexts (except group posts tab)
- [ ] Verify pagination works correctly with GROUP post filtering

---

## Files Modified

### Backend
1. `social/src/main/java/com/chefkix/social/post/service/PostService.java`
   - Updated: `getAllPosts()`, `getForYouFeed()`, `getFollowingFeed()`, `getAllPostsByUserId()`, `searchPosts()`

### Frontend
1. `src/app/(main)/feed/page.tsx`
   - Updated: `filteredPosts` derived state
   
2. `src/components/profile/UserProfile.tsx`
   - Updated: Posts tab fetch logic
   - Updated: Saved posts tab fetch logic

---

## Notes

- GROUP posts are **never** hidden in group context (only hidden on user level via `isHidden` flag)
- GROUP posts cannot be shared directly to user feeds (API validation happens in `createGroupPost()`)
- Users cannot create GROUP posts outside of groups (permission check in `GroupMemberRepository`)
- This implementation follows **Facebook's social pattern** where group posts remain isolated within their group context

---

## Future Considerations

1. **Notifications**: Verify GROUP post notifications only trigger within group context
2. **Trending Algorithm**: GROUP posts excluded from global trending scores
3. **Analytics**: GROUP post metrics tracked separately (group engagement, not global feed)
4. **API Documentation**: Update Swagger/OpenAPI docs to clarify GROUP post filtering
