package org.teacon.slides.http.impl.client.cache.memcached;

import java.io.Serial;

public class MemcachedKeyHashingException extends RuntimeException {
   @Serial
   private static final long serialVersionUID = -7553380015989141114L;

   public MemcachedKeyHashingException(Throwable cause) {
      super(cause);
   }
}
