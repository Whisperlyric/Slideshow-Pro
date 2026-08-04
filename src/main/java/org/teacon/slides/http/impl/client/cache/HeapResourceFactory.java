package org.teacon.slides.http.impl.client.cache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.teacon.slides.http.client.cache.InputLimit;
import org.teacon.slides.http.client.cache.Resource;
import org.teacon.slides.http.client.cache.ResourceFactory;

@Contract(
   threading = ThreadingBehavior.IMMUTABLE
)
public class HeapResourceFactory implements ResourceFactory {
   @Override
   public Resource generate(String requestId, InputStream inStream, InputLimit limit) throws IOException {
      ByteArrayOutputStream outStream = new ByteArrayOutputStream();
      byte[] buf = new byte[2048];
      long total = 0L;

      int l;
      while ((l = inStream.read(buf)) != -1) {
         outStream.write(buf, 0, l);
         total += (long)l;
         if (limit != null && total > limit.getValue()) {
            limit.reached();
            break;
         }
      }

      return this.createResource(outStream.toByteArray());
   }

   @Override
   public Resource copy(String requestId, Resource resource) throws IOException {
      byte[] body;
      if (resource instanceof HeapResource) {
         body = ((HeapResource)resource).getByteArray();
      } else {
         ByteArrayOutputStream outStream = new ByteArrayOutputStream();
         IOUtils.copyAndClose(resource.getInputStream(), outStream);
         body = outStream.toByteArray();
      }

      return this.createResource(body);
   }

   Resource createResource(byte[] buf) {
      return new HeapResource(buf);
   }
}
