package org.teacon.slides.http.client.cache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface HttpCacheEntrySerializer {
   void writeTo(HttpCacheEntry var1, OutputStream var2) throws IOException;

   HttpCacheEntry readFrom(InputStream var1) throws IOException;
}
