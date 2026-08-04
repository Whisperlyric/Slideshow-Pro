package org.teacon.slides.http.impl.client.cache;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.util.Args;
import org.teacon.slides.http.client.cache.Resource;

class CombinedEntity extends AbstractHttpEntity {
   private final Resource resource;
   private final InputStream combinedStream;

   CombinedEntity(Resource resource, InputStream inStream) throws IOException {
      this.resource = resource;
      this.combinedStream = new SequenceInputStream(new CombinedEntity.ResourceStream(resource.getInputStream()), inStream);
   }

   public long getContentLength() {
      return -1L;
   }

   public boolean isRepeatable() {
      return false;
   }

   public boolean isStreaming() {
      return true;
   }

   public InputStream getContent() throws IOException, IllegalStateException {
      return this.combinedStream;
   }

   public void writeTo(OutputStream outStream) throws IOException {
      Args.notNull(outStream, "Output stream");
      InputStream inStream = this.getContent();

      try {
         byte[] tmp = new byte[2048];

         int l;
         while ((l = inStream.read(tmp)) != -1) {
            outStream.write(tmp, 0, l);
         }
      } finally {
         inStream.close();
      }
   }

   private void dispose() {
      this.resource.dispose();
   }

   class ResourceStream extends FilterInputStream {
      protected ResourceStream(InputStream in) {
         super(in);
      }

      @Override
      public void close() throws IOException {
         try {
            super.close();
         } finally {
            CombinedEntity.this.dispose();
         }
      }
   }
}
