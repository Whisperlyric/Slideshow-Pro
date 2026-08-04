package org.teacon.slides.http.impl.client.cache;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.util.Args;
import org.teacon.slides.http.client.cache.HttpCacheEntry;
import org.teacon.slides.http.client.cache.HttpCacheStorage;
import org.teacon.slides.http.client.cache.HttpCacheUpdateCallback;
import org.teacon.slides.http.client.cache.Resource;

@Contract(
   threading = ThreadingBehavior.SAFE
)
public class ManagedHttpCacheStorage implements HttpCacheStorage, Closeable {
   private final CacheMap entries;
   private final ReferenceQueue<HttpCacheEntry> morque;
   private final Set<ResourceReference> resources;
   private final AtomicBoolean active;

   public ManagedHttpCacheStorage(CacheConfig config) {
      this.entries = new CacheMap(config.getMaxCacheEntries());
      this.morque = new ReferenceQueue<>();
      this.resources = new HashSet<>();
      this.active = new AtomicBoolean(true);
   }

   private void ensureValidState() throws IllegalStateException {
      if (!this.active.get()) {
         throw new IllegalStateException("Cache has been shut down");
      }
   }

   private void keepResourceReference(HttpCacheEntry entry) {
      Resource resource = entry.getResource();
      if (resource != null) {
         ResourceReference ref = new ResourceReference(entry, this.morque);
         this.resources.add(ref);
      }
   }

   @Override
   public void putEntry(String url, HttpCacheEntry entry) throws IOException {
      Args.notNull(url, "URL");
      Args.notNull(entry, "Cache entry");
      this.ensureValidState();
      synchronized (this) {
         this.entries.put(url, entry);
         this.keepResourceReference(entry);
      }
   }

   @Override
   public HttpCacheEntry getEntry(String url) throws IOException {
      Args.notNull(url, "URL");
      this.ensureValidState();
      synchronized (this) {
         return this.entries.get(url);
      }
   }

   @Override
   public void removeEntry(String url) throws IOException {
      Args.notNull(url, "URL");
      this.ensureValidState();
      synchronized (this) {
         this.entries.remove(url);
      }
   }

   @Override
   public void updateEntry(String url, HttpCacheUpdateCallback callback) throws IOException {
      Args.notNull(url, "URL");
      Args.notNull(callback, "Callback");
      this.ensureValidState();
      synchronized (this) {
         HttpCacheEntry existing = this.entries.get(url);
         HttpCacheEntry updated = callback.update(existing);
         this.entries.put(url, updated);
         if (existing != updated) {
            this.keepResourceReference(updated);
         }
      }
   }

   public void cleanResources() {
      ResourceReference ref;
      if (this.active.get()) {
         while ((ref = (ResourceReference)this.morque.poll()) != null) {
            synchronized (this) {
               this.resources.remove(ref);
            }

            ref.getResource().dispose();
         }
      }
   }

   public void shutdown() {
      if (this.active.compareAndSet(true, false)) {
         synchronized (this) {
            this.entries.clear();

            for (ResourceReference ref : this.resources) {
               ref.getResource().dispose();
            }

            this.resources.clear();

            while (this.morque.poll() != null) {
            }
         }
      }
   }

   @Override
   public void close() {
      if (this.active.compareAndSet(true, false)) {
         ResourceReference ref;
         synchronized (this) {
            while ((ref = (ResourceReference)this.morque.poll()) != null) {
               this.resources.remove(ref);
               ref.getResource().dispose();
            }
         }
      }
   }
}
