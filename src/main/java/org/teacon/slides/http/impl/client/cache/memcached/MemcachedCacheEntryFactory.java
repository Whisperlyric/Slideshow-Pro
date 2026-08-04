package org.teacon.slides.http.impl.client.cache.memcached;

import org.teacon.slides.http.client.cache.HttpCacheEntry;

public interface MemcachedCacheEntryFactory {
   MemcachedCacheEntry getMemcachedCacheEntry(String var1, HttpCacheEntry var2);

   MemcachedCacheEntry getUnsetCacheEntry();
}
