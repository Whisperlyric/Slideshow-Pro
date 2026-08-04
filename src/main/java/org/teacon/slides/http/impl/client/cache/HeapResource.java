package org.teacon.slides.http.impl.client.cache;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.teacon.slides.http.client.cache.Resource;

@Contract(
   threading = ThreadingBehavior.IMMUTABLE
)
public class HeapResource implements Resource {
   private static final long serialVersionUID = -2078599905620463394L;
   private final byte[] b;

   public HeapResource(byte[] b) {
      this.b = b;
   }

   byte[] getByteArray() {
      return this.b;
   }

   @Override
   public InputStream getInputStream() {
      return new ByteArrayInputStream(this.b);
   }

   @Override
   public long length() {
      return (long)this.b.length;
   }

   @Override
   public void dispose() {
   }
}
