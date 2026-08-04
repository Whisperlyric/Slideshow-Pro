package org.teacon.slides.cache;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import org.apache.http.util.Args;
import org.teacon.slides.http.client.cache.HttpCacheEntry;
import org.teacon.slides.http.client.cache.Resource;

final class ResourceReference extends PhantomReference<HttpCacheEntry> {
   private final Resource resource;

   public ResourceReference(HttpCacheEntry entry, ReferenceQueue<HttpCacheEntry> q) {
      super(entry, q);
      this.resource = (Resource)Args.notNull(entry.getResource(), "Resource");
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
      return obj == this;
   }
}
