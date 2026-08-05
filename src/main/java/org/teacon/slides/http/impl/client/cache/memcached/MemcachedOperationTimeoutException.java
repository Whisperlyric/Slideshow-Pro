package org.teacon.slides.http.impl.client.cache.memcached;

import java.io.IOException;
import java.io.Serial;

class MemcachedOperationTimeoutException extends IOException {
   @Serial
   private static final long serialVersionUID = 1608334789051537010L;

   public MemcachedOperationTimeoutException(Throwable cause) {
      super(cause.getMessage());
      this.initCause(cause);
   }
}
