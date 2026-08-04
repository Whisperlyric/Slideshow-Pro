package org.teacon.slides.http.client.cache;

public class InputLimit {
   private final long value;
   private boolean reached;

   public InputLimit(long value) {
      this.value = value;
      this.reached = false;
   }

   public long getValue() {
      return this.value;
   }

   public void reached() {
      this.reached = true;
   }

   public boolean isReached() {
      return this.reached;
   }
}
