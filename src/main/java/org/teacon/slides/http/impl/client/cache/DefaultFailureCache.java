package org.teacon.slides.http.impl.client.cache;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;

@Contract(
   threading = ThreadingBehavior.SAFE
)
public class DefaultFailureCache implements FailureCache {
   static final int DEFAULT_MAX_SIZE = 1000;
   static final int MAX_UPDATE_TRIES = 10;
   private final int maxSize;
   private final ConcurrentMap<String, FailureCacheValue> storage;

   public DefaultFailureCache() {
      this(1000);
   }

   public DefaultFailureCache(int maxSize) {
      this.maxSize = maxSize;
      this.storage = new ConcurrentHashMap<>();
   }

   @Override
   public int getErrorCount(String identifier) {
      if (identifier == null) {
         throw new IllegalArgumentException("identifier may not be null");
      } else {
         FailureCacheValue storedErrorCode = this.storage.get(identifier);
         return storedErrorCode != null ? storedErrorCode.getErrorCount() : 0;
      }
   }

   @Override
   public void resetErrorCount(String identifier) {
      if (identifier == null) {
         throw new IllegalArgumentException("identifier may not be null");
      } else {
         this.storage.remove(identifier);
      }
   }

   @Override
   public void increaseErrorCount(String identifier) {
      if (identifier == null) {
         throw new IllegalArgumentException("identifier may not be null");
      } else {
         this.updateValue(identifier);
         this.removeOldestEntryIfMapSizeExceeded();
      }
   }

   private void updateValue(String identifier) {
      for (int i = 0; i < 10; i++) {
         FailureCacheValue oldValue = this.storage.get(identifier);
         if (oldValue == null) {
            FailureCacheValue newValue = new FailureCacheValue(identifier, 1);
            if (this.storage.putIfAbsent(identifier, newValue) == null) {
               return;
            }
         } else {
            int errorCount = oldValue.getErrorCount();
            if (errorCount == Integer.MAX_VALUE) {
               return;
            }

            FailureCacheValue newValue = new FailureCacheValue(identifier, errorCount + 1);
            if (this.storage.replace(identifier, oldValue, newValue)) {
               return;
            }
         }
      }
   }

   private void removeOldestEntryIfMapSizeExceeded() {
      if (this.storage.size() > this.maxSize) {
         FailureCacheValue valueWithOldestTimestamp = this.findValueWithOldestTimestamp();
         if (valueWithOldestTimestamp != null) {
            this.storage.remove(valueWithOldestTimestamp.getKey(), valueWithOldestTimestamp);
         }
      }
   }

   private FailureCacheValue findValueWithOldestTimestamp() {
      long oldestTimestamp = Long.MAX_VALUE;
      FailureCacheValue oldestValue = null;

      for (Entry<String, FailureCacheValue> storageEntry : this.storage.entrySet()) {
         FailureCacheValue value = storageEntry.getValue();
         long creationTimeInNanos = value.getCreationTimeInNanos();
         if (creationTimeInNanos < oldestTimestamp) {
            oldestTimestamp = creationTimeInNanos;
            oldestValue = storageEntry.getValue();
         }
      }

      return oldestValue;
   }
}
