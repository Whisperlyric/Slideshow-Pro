package org.teacon.slides.http.impl.client.cache.memcached;

import java.io.Serial;

public class MemcachedSerializationException extends RuntimeException {
   @Serial
   private static final long serialVersionUID = 2201652990656412236L;

   public MemcachedSerializationException(Throwable cause) {
      super(cause);
   }
}
