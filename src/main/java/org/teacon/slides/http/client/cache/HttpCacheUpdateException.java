package org.teacon.slides.http.client.cache;

import java.io.Serial;

public class HttpCacheUpdateException extends Exception {
   @Serial
   private static final long serialVersionUID = 823573584868632876L;

   public HttpCacheUpdateException(String message) {
      super(message);
   }

   public HttpCacheUpdateException(String message, Throwable cause) {
      super(message);
      this.initCause(cause);
   }
}
