package org.teacon.slides.http.impl.client.cache;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import org.teacon.slides.http.client.cache.HttpCacheEntry;

final class CacheMap extends LinkedHashMap<String, HttpCacheEntry> {
   private static final long serialVersionUID = -7750025207539768511L;
   private final int maxEntries;

   CacheMap(int maxEntries) {
      super(20, 0.75F, true);
      this.maxEntries = maxEntries;
   }

   @Override
   protected boolean removeEldestEntry(Entry<String, HttpCacheEntry> eldest) {
      return this.size() > this.maxEntries;
   }
}
