package org.teacon.slides.http.client.cache;

import java.io.IOException;
import java.io.Serial;

public class HttpCacheEntrySerializationException extends IOException {
   @Serial
   private static final long serialVersionUID = 9219188365878433519L;

   public HttpCacheEntrySerializationException(String message) {
      super(message);
   }

   public HttpCacheEntrySerializationException(String message, Throwable cause) {
      super(message);
      this.initCause(cause);
   }
}
