package org.teacon.slides.http.impl.client.cache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.util.Args;
import org.teacon.slides.http.client.cache.HttpCacheEntry;

@Contract(
   threading = ThreadingBehavior.IMMUTABLE
)
class CacheEntity implements HttpEntity, Serializable {
   private static final long serialVersionUID = -3467082284120936233L;
   private final HttpCacheEntry cacheEntry;

   public CacheEntity(HttpCacheEntry cacheEntry) {
      this.cacheEntry = cacheEntry;
   }

   public Header getContentType() {
      return this.cacheEntry.getFirstHeader("Content-Type");
   }

   public Header getContentEncoding() {
      return this.cacheEntry.getFirstHeader("Content-Encoding");
   }

   public boolean isChunked() {
      return false;
   }

   public boolean isRepeatable() {
      return true;
   }

   public long getContentLength() {
      return this.cacheEntry.getResource().length();
   }

   public InputStream getContent() throws IOException {
      return this.cacheEntry.getResource().getInputStream();
   }

   public void writeTo(OutputStream outStream) throws IOException {
      Args.notNull(outStream, "Output stream");
      InputStream inStream = this.cacheEntry.getResource().getInputStream();

      try {
         IOUtils.copy(inStream, outStream);
      } finally {
         inStream.close();
      }
   }

   public boolean isStreaming() {
      return false;
   }

   public void consumeContent() throws IOException {
   }
}
