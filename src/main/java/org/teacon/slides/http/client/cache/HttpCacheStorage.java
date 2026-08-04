package org.teacon.slides.http.client.cache;

import java.io.IOException;

public interface HttpCacheStorage {
   void putEntry(String var1, HttpCacheEntry var2) throws IOException;

   HttpCacheEntry getEntry(String var1) throws IOException;

   void removeEntry(String var1) throws IOException;

   void updateEntry(String var1, HttpCacheUpdateCallback var2) throws IOException, HttpCacheUpdateException;
}
