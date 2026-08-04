package org.teacon.slides.http.impl.client.cache;

import org.apache.http.util.Args;

public class CacheConfig implements Cloneable {
   public static final int DEFAULT_MAX_OBJECT_SIZE_BYTES = 8192;
   public static final int DEFAULT_MAX_CACHE_ENTRIES = 1000;
   public static final int DEFAULT_MAX_UPDATE_RETRIES = 1;
   public static final boolean DEFAULT_303_CACHING_ENABLED = false;
   public static final boolean DEFAULT_WEAK_ETAG_ON_PUTDELETE_ALLOWED = false;
   public static final boolean DEFAULT_HEURISTIC_CACHING_ENABLED = false;
   public static final float DEFAULT_HEURISTIC_COEFFICIENT = 0.1F;
   public static final long DEFAULT_HEURISTIC_LIFETIME = 0L;
   public static final int DEFAULT_ASYNCHRONOUS_WORKERS_MAX = 1;
   public static final int DEFAULT_ASYNCHRONOUS_WORKERS_CORE = 1;
   public static final int DEFAULT_ASYNCHRONOUS_WORKER_IDLE_LIFETIME_SECS = 60;
   public static final int DEFAULT_REVALIDATION_QUEUE_SIZE = 100;
   public static final CacheConfig DEFAULT = new CacheConfig.Builder().build();
   private long maxObjectSize;
   private int maxCacheEntries;
   private int maxUpdateRetries;
   private final boolean allow303Caching;
   private final boolean weakETagOnPutDeleteAllowed;
   private boolean heuristicCachingEnabled;
   private float heuristicCoefficient;
   private long heuristicDefaultLifetime;
   private boolean isSharedCache;
   private int asynchronousWorkersMax;
   private int asynchronousWorkersCore;
   private int asynchronousWorkerIdleLifetimeSecs;
   private int revalidationQueueSize;
   private boolean neverCacheHTTP10ResponsesWithQuery;

   @Deprecated
   public CacheConfig() {
      this.maxObjectSize = 8192L;
      this.maxCacheEntries = 1000;
      this.maxUpdateRetries = 1;
      this.allow303Caching = false;
      this.weakETagOnPutDeleteAllowed = false;
      this.heuristicCachingEnabled = false;
      this.heuristicCoefficient = 0.1F;
      this.heuristicDefaultLifetime = 0L;
      this.isSharedCache = true;
      this.asynchronousWorkersMax = 1;
      this.asynchronousWorkersCore = 1;
      this.asynchronousWorkerIdleLifetimeSecs = 60;
      this.revalidationQueueSize = 100;
   }

   CacheConfig(
      long maxObjectSize,
      int maxCacheEntries,
      int maxUpdateRetries,
      boolean allow303Caching,
      boolean weakETagOnPutDeleteAllowed,
      boolean heuristicCachingEnabled,
      float heuristicCoefficient,
      long heuristicDefaultLifetime,
      boolean isSharedCache,
      int asynchronousWorkersMax,
      int asynchronousWorkersCore,
      int asynchronousWorkerIdleLifetimeSecs,
      int revalidationQueueSize,
      boolean neverCacheHTTP10ResponsesWithQuery
   ) {
      this.maxObjectSize = maxObjectSize;
      this.maxCacheEntries = maxCacheEntries;
      this.maxUpdateRetries = maxUpdateRetries;
      this.allow303Caching = allow303Caching;
      this.weakETagOnPutDeleteAllowed = weakETagOnPutDeleteAllowed;
      this.heuristicCachingEnabled = heuristicCachingEnabled;
      this.heuristicCoefficient = heuristicCoefficient;
      this.heuristicDefaultLifetime = heuristicDefaultLifetime;
      this.isSharedCache = isSharedCache;
      this.asynchronousWorkersMax = asynchronousWorkersMax;
      this.asynchronousWorkersCore = asynchronousWorkersCore;
      this.asynchronousWorkerIdleLifetimeSecs = asynchronousWorkerIdleLifetimeSecs;
      this.revalidationQueueSize = revalidationQueueSize;
   }

   @Deprecated
   public int getMaxObjectSizeBytes() {
      return this.maxObjectSize > 2147483647L ? Integer.MAX_VALUE : (int)this.maxObjectSize;
   }

   @Deprecated
   public void setMaxObjectSizeBytes(int maxObjectSizeBytes) {
      if (maxObjectSizeBytes > Integer.MAX_VALUE) {
         this.maxObjectSize = 2147483647L;
      } else {
         this.maxObjectSize = (long)maxObjectSizeBytes;
      }
   }

   public long getMaxObjectSize() {
      return this.maxObjectSize;
   }

   @Deprecated
   public void setMaxObjectSize(long maxObjectSize) {
      this.maxObjectSize = maxObjectSize;
   }

   public boolean isNeverCacheHTTP10ResponsesWithQuery() {
      return this.neverCacheHTTP10ResponsesWithQuery;
   }

   public int getMaxCacheEntries() {
      return this.maxCacheEntries;
   }

   @Deprecated
   public void setMaxCacheEntries(int maxCacheEntries) {
      this.maxCacheEntries = maxCacheEntries;
   }

   public int getMaxUpdateRetries() {
      return this.maxUpdateRetries;
   }

   @Deprecated
   public void setMaxUpdateRetries(int maxUpdateRetries) {
      this.maxUpdateRetries = maxUpdateRetries;
   }

   public boolean is303CachingEnabled() {
      return this.allow303Caching;
   }

   public boolean isWeakETagOnPutDeleteAllowed() {
      return this.weakETagOnPutDeleteAllowed;
   }

   public boolean isHeuristicCachingEnabled() {
      return this.heuristicCachingEnabled;
   }

   @Deprecated
   public void setHeuristicCachingEnabled(boolean heuristicCachingEnabled) {
      this.heuristicCachingEnabled = heuristicCachingEnabled;
   }

   public float getHeuristicCoefficient() {
      return this.heuristicCoefficient;
   }

   @Deprecated
   public void setHeuristicCoefficient(float heuristicCoefficient) {
      this.heuristicCoefficient = heuristicCoefficient;
   }

   public long getHeuristicDefaultLifetime() {
      return this.heuristicDefaultLifetime;
   }

   @Deprecated
   public void setHeuristicDefaultLifetime(long heuristicDefaultLifetimeSecs) {
      this.heuristicDefaultLifetime = heuristicDefaultLifetimeSecs;
   }

   public boolean isSharedCache() {
      return this.isSharedCache;
   }

   @Deprecated
   public void setSharedCache(boolean isSharedCache) {
      this.isSharedCache = isSharedCache;
   }

   public int getAsynchronousWorkersMax() {
      return this.asynchronousWorkersMax;
   }

   @Deprecated
   public void setAsynchronousWorkersMax(int max) {
      this.asynchronousWorkersMax = max;
   }

   public int getAsynchronousWorkersCore() {
      return this.asynchronousWorkersCore;
   }

   @Deprecated
   public void setAsynchronousWorkersCore(int min) {
      this.asynchronousWorkersCore = min;
   }

   public int getAsynchronousWorkerIdleLifetimeSecs() {
      return this.asynchronousWorkerIdleLifetimeSecs;
   }

   @Deprecated
   public void setAsynchronousWorkerIdleLifetimeSecs(int secs) {
      this.asynchronousWorkerIdleLifetimeSecs = secs;
   }

   public int getRevalidationQueueSize() {
      return this.revalidationQueueSize;
   }

   @Deprecated
   public void setRevalidationQueueSize(int size) {
      this.revalidationQueueSize = size;
   }

   protected CacheConfig clone() throws CloneNotSupportedException {
      return (CacheConfig)super.clone();
   }

   public static CacheConfig.Builder custom() {
      return new CacheConfig.Builder();
   }

   public static CacheConfig.Builder copy(CacheConfig config) {
      Args.notNull(config, "Cache config");
      return new CacheConfig.Builder()
         .setMaxObjectSize(config.getMaxObjectSize())
         .setMaxCacheEntries(config.getMaxCacheEntries())
         .setMaxUpdateRetries(config.getMaxUpdateRetries())
         .setHeuristicCachingEnabled(config.isHeuristicCachingEnabled())
         .setHeuristicCoefficient(config.getHeuristicCoefficient())
         .setHeuristicDefaultLifetime(config.getHeuristicDefaultLifetime())
         .setSharedCache(config.isSharedCache())
         .setAsynchronousWorkersMax(config.getAsynchronousWorkersMax())
         .setAsynchronousWorkersCore(config.getAsynchronousWorkersCore())
         .setAsynchronousWorkerIdleLifetimeSecs(config.getAsynchronousWorkerIdleLifetimeSecs())
         .setRevalidationQueueSize(config.getRevalidationQueueSize())
         .setNeverCacheHTTP10ResponsesWithQueryString(config.isNeverCacheHTTP10ResponsesWithQuery());
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("[maxObjectSize=")
         .append(this.maxObjectSize)
         .append(", maxCacheEntries=")
         .append(this.maxCacheEntries)
         .append(", maxUpdateRetries=")
         .append(this.maxUpdateRetries)
         .append(", 303CachingEnabled=")
         .append(this.allow303Caching)
         .append(", weakETagOnPutDeleteAllowed=")
         .append(this.weakETagOnPutDeleteAllowed)
         .append(", heuristicCachingEnabled=")
         .append(this.heuristicCachingEnabled)
         .append(", heuristicCoefficient=")
         .append(this.heuristicCoefficient)
         .append(", heuristicDefaultLifetime=")
         .append(this.heuristicDefaultLifetime)
         .append(", isSharedCache=")
         .append(this.isSharedCache)
         .append(", asynchronousWorkersMax=")
         .append(this.asynchronousWorkersMax)
         .append(", asynchronousWorkersCore=")
         .append(this.asynchronousWorkersCore)
         .append(", asynchronousWorkerIdleLifetimeSecs=")
         .append(this.asynchronousWorkerIdleLifetimeSecs)
         .append(", revalidationQueueSize=")
         .append(this.revalidationQueueSize)
         .append(", neverCacheHTTP10ResponsesWithQuery=")
         .append(this.neverCacheHTTP10ResponsesWithQuery)
         .append("]");
      return builder.toString();
   }

   public static class Builder {
      private long maxObjectSize = 8192L;
      private int maxCacheEntries = 1000;
      private int maxUpdateRetries = 1;
      private boolean allow303Caching = false;
      private boolean weakETagOnPutDeleteAllowed = false;
      private boolean heuristicCachingEnabled = false;
      private float heuristicCoefficient = 0.1F;
      private long heuristicDefaultLifetime = 0L;
      private boolean isSharedCache = true;
      private int asynchronousWorkersMax = 1;
      private int asynchronousWorkersCore = 1;
      private int asynchronousWorkerIdleLifetimeSecs = 60;
      private int revalidationQueueSize = 100;
      private boolean neverCacheHTTP10ResponsesWithQuery;

      Builder() {
      }

      public CacheConfig.Builder setMaxObjectSize(long maxObjectSize) {
         this.maxObjectSize = maxObjectSize;
         return this;
      }

      public CacheConfig.Builder setMaxCacheEntries(int maxCacheEntries) {
         this.maxCacheEntries = maxCacheEntries;
         return this;
      }

      public CacheConfig.Builder setMaxUpdateRetries(int maxUpdateRetries) {
         this.maxUpdateRetries = maxUpdateRetries;
         return this;
      }

      public CacheConfig.Builder setAllow303Caching(boolean allow303Caching) {
         this.allow303Caching = allow303Caching;
         return this;
      }

      public CacheConfig.Builder setWeakETagOnPutDeleteAllowed(boolean weakETagOnPutDeleteAllowed) {
         this.weakETagOnPutDeleteAllowed = weakETagOnPutDeleteAllowed;
         return this;
      }

      public CacheConfig.Builder setHeuristicCachingEnabled(boolean heuristicCachingEnabled) {
         this.heuristicCachingEnabled = heuristicCachingEnabled;
         return this;
      }

      public CacheConfig.Builder setHeuristicCoefficient(float heuristicCoefficient) {
         this.heuristicCoefficient = heuristicCoefficient;
         return this;
      }

      public CacheConfig.Builder setHeuristicDefaultLifetime(long heuristicDefaultLifetime) {
         this.heuristicDefaultLifetime = heuristicDefaultLifetime;
         return this;
      }

      public CacheConfig.Builder setSharedCache(boolean isSharedCache) {
         this.isSharedCache = isSharedCache;
         return this;
      }

      public CacheConfig.Builder setAsynchronousWorkersMax(int asynchronousWorkersMax) {
         this.asynchronousWorkersMax = asynchronousWorkersMax;
         return this;
      }

      public CacheConfig.Builder setAsynchronousWorkersCore(int asynchronousWorkersCore) {
         this.asynchronousWorkersCore = asynchronousWorkersCore;
         return this;
      }

      public CacheConfig.Builder setAsynchronousWorkerIdleLifetimeSecs(int asynchronousWorkerIdleLifetimeSecs) {
         this.asynchronousWorkerIdleLifetimeSecs = asynchronousWorkerIdleLifetimeSecs;
         return this;
      }

      public CacheConfig.Builder setRevalidationQueueSize(int revalidationQueueSize) {
         this.revalidationQueueSize = revalidationQueueSize;
         return this;
      }

      public CacheConfig.Builder setNeverCacheHTTP10ResponsesWithQueryString(boolean neverCacheHTTP10ResponsesWithQuery) {
         this.neverCacheHTTP10ResponsesWithQuery = neverCacheHTTP10ResponsesWithQuery;
         return this;
      }

      public CacheConfig build() {
         return new CacheConfig(
            this.maxObjectSize,
            this.maxCacheEntries,
            this.maxUpdateRetries,
            this.allow303Caching,
            this.weakETagOnPutDeleteAllowed,
            this.heuristicCachingEnabled,
            this.heuristicCoefficient,
            this.heuristicDefaultLifetime,
            this.isSharedCache,
            this.asynchronousWorkersMax,
            this.asynchronousWorkersCore,
            this.asynchronousWorkerIdleLifetimeSecs,
            this.revalidationQueueSize,
            this.neverCacheHTTP10ResponsesWithQuery
         );
      }
   }
}
