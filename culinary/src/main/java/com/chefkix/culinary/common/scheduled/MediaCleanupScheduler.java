package com.chefkix.culinary.common.scheduled;

import com.cloudinary.Cloudinary;
import com.cloudinary.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MediaCleanupScheduler {

    private final Cloudinary cloudinary;
    private final MongoTemplate mongoTemplate;

    private static final boolean DRY_RUN = true;
    private static final int MAX_ASSETS_PER_RUN = 500;
    private static final String ORPHAN_TAG = "orphan";
    private static final int MIN_AGE_DAYS = 7;

    /**
     */
    @Scheduled(cron = "0 0 3 * * SUN", zone = "UTC")
    public void cleanupOrphanMedia() {
        log.info("[MediaCleanup] Starting weekly orphan media cleanup (dryRun={})", DRY_RUN);

        try {
            int orphansTagged = tagNewOrphans();

            int orphansDeleted = deleteConfirmedOrphans();

            log.info("[MediaCleanup] Complete — tagged: {}, deleted: {} (dryRun={})",
                    orphansTagged, orphansDeleted, DRY_RUN);

            if (orphansTagged > 1000) {
                log.warn("[MediaCleanup] ALERT: High orphan count ({}) — possible excessive orphan generation", orphansTagged);
            }
        } catch (Exception e) {
            log.error("[MediaCleanup] Failed", e);
        }
    }

    /**
     */
    @SuppressWarnings("unchecked")
    private int tagNewOrphans() {
        try {
            Instant cutoff = Instant.now().minus(MIN_AGE_DAYS, ChronoUnit.DAYS);
String cutoffStr = cutoff.toString().substring(0, 10);

            Map<String, Object> params = Map.of(
                    "type", "upload",
                    "prefix", "chefkix/",
                    "max_results", MAX_ASSETS_PER_RUN,
"direction", "asc"
            );

            ApiResponse response = cloudinary.api().resources(params);
            List<Map<String, Object>> resources = (List<Map<String, Object>>) response.get("resources");

            if (resources == null || resources.isEmpty()) {
                log.info("[MediaCleanup] No assets found in chefkix/ folder");
                return 0;
            }

            int orphansTagged = 0;
            Set<String> allUrls = collectAllReferencedUrls();

            for (Map<String, Object> resource : resources) {
                String publicId = (String) resource.get("public_id");
                String secureUrl = (String) resource.get("secure_url");
                String createdAt = (String) resource.get("created_at");
                List<String> tags = resource.get("tags") != null
                        ? (List<String>) resource.get("tags")
                        : Collections.emptyList();

                if (tags.contains(ORPHAN_TAG)) continue;

                if (createdAt != null && createdAt.compareTo(cutoffStr) > 0) continue;

                if (!isUrlReferenced(secureUrl, allUrls)) {
                    orphansTagged++;
                    if (DRY_RUN) {
                        log.info("[MediaCleanup] DRY-RUN would tag orphan: {} (created: {})", publicId, createdAt);
                    } else {
                        cloudinary.uploader().addTag(ORPHAN_TAG, new String[]{publicId}, null);
                        log.info("[MediaCleanup] Tagged orphan: {} (created: {})", publicId, createdAt);
                    }
                }
            }

            return orphansTagged;
        } catch (Exception e) {
            log.error("[MediaCleanup] Pass 1 (tag) failed", e);
            return 0;
        }
    }

    /**
     */
    @SuppressWarnings("unchecked")
    private int deleteConfirmedOrphans() {
        try {
            Map<String, Object> params = Map.of(
                    "tag", ORPHAN_TAG,
                    "type", "upload",
                    "max_results", MAX_ASSETS_PER_RUN
            );

            ApiResponse response = cloudinary.api().resourcesByTag(ORPHAN_TAG, params);
            List<Map<String, Object>> resources = (List<Map<String, Object>>) response.get("resources");

            if (resources == null || resources.isEmpty()) {
                log.info("[MediaCleanup] No previously tagged orphans to clean up");
                return 0;
            }

            int deleted = 0;
            Set<String> allUrls = collectAllReferencedUrls();

            for (Map<String, Object> resource : resources) {
                String publicId = (String) resource.get("public_id");
                String secureUrl = (String) resource.get("secure_url");
                String resourceType = (String) resource.get("resource_type");

                if (isUrlReferenced(secureUrl, allUrls)) {
                    if (!DRY_RUN) {
                        cloudinary.uploader().removeTag(ORPHAN_TAG, new String[]{publicId}, null);
                    }
                    log.info("[MediaCleanup] Un-orphaned (now referenced): {}", publicId);
                    continue;
                }

                deleted++;
                if (DRY_RUN) {
                    log.info("[MediaCleanup] DRY-RUN would delete: {} (type: {})", publicId, resourceType);
                } else {
                    Map<String, Object> destroyParams = new HashMap<>();
                    destroyParams.put("resource_type", resourceType != null ? resourceType : "image");
                    cloudinary.uploader().destroy(publicId, destroyParams);
                    log.info("[MediaCleanup] Deleted orphan: {} (type: {})", publicId, resourceType);
                }
            }

            return deleted;
        } catch (Exception e) {
            log.error("[MediaCleanup] Pass 2 (delete) failed", e);
            return 0;
        }
    }

    /**
     */
    private Set<String> collectAllReferencedUrls() {
        Set<String> urls = new HashSet<>();

        mongoTemplate.find(new Query(), org.bson.Document.class, "recipes").forEach(doc -> {
            List<String> coverUrls = doc.getList("coverImageUrl", String.class);
            if (coverUrls != null) urls.addAll(coverUrls);

            List<org.bson.Document> steps = doc.getList("steps", org.bson.Document.class);
            if (steps != null) {
                for (org.bson.Document step : steps) {
                    String imgUrl = step.getString("imageUrl");
                    if (imgUrl != null) urls.add(imgUrl);
                    String vidUrl = step.getString("videoUrl");
                    if (vidUrl != null) urls.add(vidUrl);
                    String thumbUrl = step.getString("videoThumbnailUrl");
                    if (thumbUrl != null) urls.add(thumbUrl);
                }
            }
        });

        mongoTemplate.find(new Query(), org.bson.Document.class, "posts").forEach(doc -> {
            List<String> photoUrls = doc.getList("photoUrls", String.class);
            if (photoUrls != null) urls.addAll(photoUrls);
            String videoUrl = doc.getString("videoUrl");
            if (videoUrl != null) urls.add(videoUrl);
        });

        mongoTemplate.find(
                Query.query(Criteria.where("avatarUrl").exists(true)),
                org.bson.Document.class,
                "user_profiles"
        ).forEach(doc -> {
            String avatarUrl = doc.getString("avatarUrl");
            if (avatarUrl != null) urls.add(avatarUrl);
        });

        log.info("[MediaCleanup] Found {} referenced URLs across database", urls.size());
        return urls;
    }

    /**
     */
    private boolean isUrlReferenced(String url, Set<String> referencedUrls) {
        if (url == null) return false;
        if (referencedUrls.contains(url)) return true;
        if (url.startsWith("https://")) {
            return referencedUrls.contains(url.replace("https://", "http://"));
        }
        if (url.startsWith("http://")) {
            return referencedUrls.contains(url.replace("http://", "https://"));
        }
        return false;
    }
}
