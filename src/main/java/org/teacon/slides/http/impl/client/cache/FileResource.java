package org.teacon.slides.http.impl.client.cache;

import java.io.*;

import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.teacon.slides.http.client.cache.Resource;

@Contract(
   threading = ThreadingBehavior.SAFE
)
public class FileResource implements Resource {
   @Serial
   private static final long serialVersionUID = 4132244415919043397L;
   private final File file;
   private volatile boolean disposed;

   public FileResource(File file) {
      this.file = file;
      this.disposed = false;
   }

   synchronized File getFile() {
      return this.file;
   }

   @Override
   public synchronized InputStream getInputStream() throws IOException {
      return new FileInputStream(this.file);
   }

   @Override
   public synchronized long length() {
      return this.file.length();
   }

   @Override
   public synchronized void dispose() {
      if (!this.disposed) {
         this.disposed = true;
         this.file.delete();
      }
   }
}
