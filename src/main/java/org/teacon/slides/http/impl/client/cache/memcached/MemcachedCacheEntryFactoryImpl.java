package org.teacon.slides.http.impl.client.cache.memcached;

import org.teacon.slides.http.client.cache.HttpCacheEntry;

public class MemcachedCacheEntryFactoryImpl implements MemcachedCacheEntryFactory {
   @Override
   public MemcachedCacheEntry getMemcachedCacheEntry(String key, HttpCacheEntry entry) {
      return new MemcachedCacheEntryImpl(key, entry);
   }

   @Override
   public MemcachedCacheEntry getUnsetCacheEntry() {
      return new MemcachedCacheEntryImpl(null, null);
   }
}
