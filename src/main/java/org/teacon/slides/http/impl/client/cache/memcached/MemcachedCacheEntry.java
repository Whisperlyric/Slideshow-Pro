package org.teacon.slides.http.impl.client.cache.memcached;

import org.teacon.slides.http.client.cache.HttpCacheEntry;

public interface MemcachedCacheEntry {
   byte[] toByteArray();

   String getStorageKey();

   HttpCacheEntry getHttpCacheEntry();

   void set(byte[] var1);
}
