package org.teacon.slides.http.client.cache;

import java.io.IOException;

public interface HttpCacheUpdateCallback {
   HttpCacheEntry update(HttpCacheEntry var1) throws IOException;
}
