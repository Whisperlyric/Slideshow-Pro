package org.teacon.slides.http.impl.client.cache;

import java.io.IOException;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.teacon.slides.http.client.cache.HttpCacheEntry;
import org.teacon.slides.http.client.cache.HttpCacheStorage;
import org.teacon.slides.http.client.cache.HttpCacheUpdateCallback;

@Contract(
   threading = ThreadingBehavior.SAFE
)
public class BasicHttpCacheStorage implements HttpCacheStorage {
   private final CacheMap entries;

   public BasicHttpCacheStorage(CacheConfig config) {
      this.entries = new CacheMap(config.getMaxCacheEntries());
   }

   @Override
   public synchronized void putEntry(String url, HttpCacheEntry entry) throws IOException {
      this.entries.put(url, entry);
   }

   @Override
   public synchronized HttpCacheEntry getEntry(String url) throws IOException {
      return this.entries.get(url);
   }

   @Override
   public synchronized void removeEntry(String url) throws IOException {
      this.entries.remove(url);
   }

   @Override
   public synchronized void updateEntry(String url, HttpCacheUpdateCallback callback) throws IOException {
      HttpCacheEntry existingEntry = this.entries.get(url);
      this.entries.put(url, callback.update(existingEntry));
   }
}
