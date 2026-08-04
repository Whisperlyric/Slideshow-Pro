package org.teacon.slides.http.impl.client.cache;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.util.Args;
import org.teacon.slides.http.client.cache.HttpCacheEntry;
import org.teacon.slides.http.client.cache.Resource;

@Contract(
   threading = ThreadingBehavior.IMMUTABLE
)
class ResourceReference extends PhantomReference<HttpCacheEntry> {
   private final Resource resource;

   public ResourceReference(HttpCacheEntry entry, ReferenceQueue<HttpCacheEntry> q) {
      super(entry, q);
      Args.notNull(entry.getResource(), "Resource");
      this.resource = entry.getResource();
   }

   public Resource getResource() {
      return this.resource;
   }

   @Override
   public int hashCode() {
      return this.resource.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      return this.resource.equals(obj);
   }
}
