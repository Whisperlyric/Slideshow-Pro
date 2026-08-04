package org.teacon.slides.http.impl.client.cache;

import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;

@Contract(
   threading = ThreadingBehavior.IMMUTABLE
)
public class FailureCacheValue {
   private final long creationTimeInNanos = System.nanoTime();
   private final String key;
   private final int errorCount;

   public FailureCacheValue(String key, int errorCount) {
      this.key = key;
      this.errorCount = errorCount;
   }

   public long getCreationTimeInNanos() {
      return this.creationTimeInNanos;
   }

   public String getKey() {
      return this.key;
   }

   public int getErrorCount() {
      return this.errorCount;
   }

   @Override
   public String toString() {
      return "[entry creationTimeInNanos=" + this.creationTimeInNanos + "; " + "key=" + this.key + "; errorCount=" + this.errorCount + ']';
   }
}
