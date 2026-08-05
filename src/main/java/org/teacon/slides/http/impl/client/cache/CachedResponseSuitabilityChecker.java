package org.teacon.slides.http.impl.client.cache;

import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.client.utils.DateUtils;
import org.teacon.slides.http.client.cache.HttpCacheEntry;

@Contract(
   threading = ThreadingBehavior.IMMUTABLE_CONDITIONAL
)
class CachedResponseSuitabilityChecker {
   private final Log log = LogFactory.getLog(this.getClass());
   private final boolean sharedCache;
   private final boolean useHeuristicCaching;
   private final float heuristicCoefficient;
   private final long heuristicDefaultLifetime;
   private final CacheValidityPolicy validityStrategy;

   CachedResponseSuitabilityChecker(CacheValidityPolicy validityStrategy, CacheConfig config) {
      this.validityStrategy = validityStrategy;
      this.sharedCache = config.isSharedCache();
      this.useHeuristicCaching = config.isHeuristicCachingEnabled();
      this.heuristicCoefficient = config.getHeuristicCoefficient();
      this.heuristicDefaultLifetime = config.getHeuristicDefaultLifetime();
   }

   CachedResponseSuitabilityChecker(CacheConfig config) {
      this(new CacheValidityPolicy(), config);
   }

   private boolean isFreshEnough(HttpCacheEntry entry, HttpRequest request, Date now) {
      if (this.validityStrategy.isResponseFresh(entry, now)) {
         return true;
      } else if (this.useHeuristicCaching
         && this.validityStrategy.isResponseHeuristicallyFresh(entry, now, this.heuristicCoefficient, this.heuristicDefaultLifetime)) {
         return true;
      } else if (this.originInsistsOnFreshness(entry)) {
         return false;
      } else {
         long maxstale = this.getMaxStale(request);
         return maxstale != -1L && maxstale > this.validityStrategy.getStalenessSecs(entry, now);
      }
   }

   private boolean originInsistsOnFreshness(HttpCacheEntry entry) {
      if (this.validityStrategy.mustRevalidate(entry)) {
         return true;
      } else {
         return !this.sharedCache ? false : this.validityStrategy.proxyRevalidate(entry) || this.validityStrategy.hasCacheControlDirective(entry, "s-maxage");
      }
   }

   private long getMaxStale(HttpRequest request) {
      long maxstale = -1L;

      for (Header h : request.getHeaders("Cache-Control")) {
         for (HeaderElement elt : h.getElements()) {
            if ("max-stale".equals(elt.getName())) {
               if ((elt.getValue() == null || "".equals(elt.getValue().trim())) && maxstale == -1L) {
                  maxstale = Long.MAX_VALUE;
               } else {
                  try {
                     long val = Long.parseLong(elt.getValue());
                     if (val < 0L) {
                        val = 0L;
                     }

                     if (maxstale == -1L || val < maxstale) {
                        maxstale = val;
                     }
                  } catch (NumberFormatException var14) {
                     maxstale = 0L;
                  }
               }
            }
         }
      }

      return maxstale;
   }

   public boolean canCachedResponseBeUsed(HttpHost host, HttpRequest request, HttpCacheEntry entry, Date now) {
      if (!this.isFreshEnough(entry, request, now)) {
         this.log.trace("Cache entry was not fresh enough");
         return false;
      } else if (this.isGet(request) && !this.validityStrategy.contentLengthHeaderMatchesActualLength(entry)) {
         this.log.debug("Cache entry Content-Length and header information do not match");
         return false;
      } else if (this.hasUnsupportedConditionalHeaders(request)) {
         this.log.debug("Request contained conditional headers we don't handle");
         return false;
      } else if (!this.isConditional(request) && entry.getStatusCode() == 304) {
         return false;
      } else if (this.isConditional(request) && !this.allConditionalsMatch(request, entry, now)) {
         return false;
      } else if (this.hasUnsupportedCacheEntryForGet(request, entry)) {
         this.log.debug("HEAD response caching enabled but the cache entry does not contain a request method, entity or a 204 response");
         return false;
      } else {
         for (Header ccHdr : request.getHeaders("Cache-Control")) {
            for (HeaderElement elt : ccHdr.getElements()) {
               if ("no-cache".equals(elt.getName())) {
                  this.log.trace("Response contained NO CACHE directive, cache was not suitable");
                  return false;
               }

               if ("no-store".equals(elt.getName())) {
                  this.log.trace("Response contained NO STORE directive, cache was not suitable");
                  return false;
               }

               if ("max-age".equals(elt.getName())) {
                  try {
                     int maxage = Integer.parseInt(elt.getValue());
                     if (this.validityStrategy.getCurrentAgeSecs(entry, now) > (long)maxage) {
                        this.log.trace("Response from cache was NOT suitable due to max age");
                        return false;
                     }
                  } catch (NumberFormatException var21) {
                     this.log.debug("Response from cache was malformed" + var21.getMessage());
                     return false;
                  }
               }

               if ("max-stale".equals(elt.getName())) {
                  try {
                     int maxstale = Integer.parseInt(elt.getValue());
                     if (this.validityStrategy.getFreshnessLifetimeSecs(entry) > (long)maxstale) {
                        this.log.trace("Response from cache was not suitable due to Max stale freshness");
                        return false;
                     }
                  } catch (NumberFormatException var20) {
                     this.log.debug("Response from cache was malformed: " + var20.getMessage());
                     return false;
                  }
               }

               if ("min-fresh".equals(elt.getName())) {
                  try {
                     long minfresh = Long.parseLong(elt.getValue());
                     if (minfresh < 0L) {
                        return false;
                     }

                     long age = this.validityStrategy.getCurrentAgeSecs(entry, now);
                     long freshness = this.validityStrategy.getFreshnessLifetimeSecs(entry);
                     if (freshness - age < minfresh) {
                        this.log.trace("Response from cache was not suitable due to min fresh freshness requirement");
                        return false;
                     }
                  } catch (NumberFormatException var19) {
                     this.log.debug("Response from cache was malformed: " + var19.getMessage());
                     return false;
                  }
               }
            }
         }

         this.log.trace("Response from cache was suitable");
         return true;
      }
   }

   private boolean isGet(HttpRequest request) {
      return request.getRequestLine().getMethod().equals("GET");
   }

   private boolean entryIsNotA204Response(HttpCacheEntry entry) {
      return entry.getStatusCode() != 204;
   }

   private boolean cacheEntryDoesNotContainMethodAndEntity(HttpCacheEntry entry) {
      return entry.getRequestMethod() == null && entry.getResource() == null;
   }

   private boolean hasUnsupportedCacheEntryForGet(HttpRequest request, HttpCacheEntry entry) {
      return this.isGet(request) && this.cacheEntryDoesNotContainMethodAndEntity(entry) && this.entryIsNotA204Response(entry);
   }

   public boolean isConditional(HttpRequest request) {
      return this.hasSupportedEtagValidator(request) || this.hasSupportedLastModifiedValidator(request);
   }

   public boolean allConditionalsMatch(HttpRequest request, HttpCacheEntry entry, Date now) {
      boolean hasEtagValidator = this.hasSupportedEtagValidator(request);
      boolean hasLastModifiedValidator = this.hasSupportedLastModifiedValidator(request);
      boolean etagValidatorMatches = hasEtagValidator && this.etagValidatorMatches(request, entry);
      boolean lastModifiedValidatorMatches = hasLastModifiedValidator && this.lastModifiedValidatorMatches(request, entry, now);
      if (!hasEtagValidator || !hasLastModifiedValidator || etagValidatorMatches && lastModifiedValidatorMatches) {
         return hasEtagValidator && !etagValidatorMatches ? false : !hasLastModifiedValidator || lastModifiedValidatorMatches;
      } else {
         return false;
      }
   }

   private boolean hasUnsupportedConditionalHeaders(HttpRequest request) {
      return request.getFirstHeader("If-Range") != null || request.getFirstHeader("If-Match") != null || this.hasValidDateField(request, "If-Unmodified-Since");
   }

   private boolean hasSupportedEtagValidator(HttpRequest request) {
      return request.containsHeader("If-None-Match");
   }

   private boolean hasSupportedLastModifiedValidator(HttpRequest request) {
      return this.hasValidDateField(request, "If-Modified-Since");
   }

   private boolean etagValidatorMatches(HttpRequest request, HttpCacheEntry entry) {
      Header etagHeader = entry.getFirstHeader("ETag");
      String etag = etagHeader != null ? etagHeader.getValue() : null;
      Header[] ifNoneMatch = request.getHeaders("If-None-Match");
      if (ifNoneMatch != null) {
         for (Header h : ifNoneMatch) {
            for (HeaderElement elt : h.getElements()) {
               String reqEtag = elt.toString();
               if ("*".equals(reqEtag) && etag != null || reqEtag.equals(etag)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private boolean lastModifiedValidatorMatches(HttpRequest request, HttpCacheEntry entry, Date now) {
      Header lastModifiedHeader = entry.getFirstHeader("Last-Modified");
      Date lastModified = null;
      if (lastModifiedHeader != null) {
         lastModified = DateUtils.parseDate(lastModifiedHeader.getValue());
      }

      if (lastModified == null) {
         return false;
      } else {
         for (Header h : request.getHeaders("If-Modified-Since")) {
            Date ifModifiedSince = DateUtils.parseDate(h.getValue());
            if (ifModifiedSince != null && (ifModifiedSince.after(now) || lastModified.after(ifModifiedSince))) {
               return false;
            }
         }

         return true;
      }
   }

   private boolean hasValidDateField(HttpRequest request, String headerName) {
      Header[] arr$ = request.getHeaders(headerName);
      int len$ = arr$.length;
      int i$ = 0;
      if (i$ < len$) {
         Header h = arr$[i$];
         Date date = DateUtils.parseDate(h.getValue());
         return date != null;
      } else {
         return false;
      }
   }
}
