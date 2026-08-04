package org.teacon.slides.http.client.cache;

import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

public class HttpCacheContext extends HttpClientContext {
   public static final String CACHE_RESPONSE_STATUS = "http.cache.response.status";

   public static HttpCacheContext adapt(HttpContext context) {
      return context instanceof HttpCacheContext ? (HttpCacheContext)context : new HttpCacheContext(context);
   }

   public static HttpCacheContext create() {
      return new HttpCacheContext(new BasicHttpContext());
   }

   public HttpCacheContext(HttpContext context) {
      super(context);
   }

   public HttpCacheContext() {
   }

   public CacheResponseStatus getCacheResponseStatus() {
      return (CacheResponseStatus)this.getAttribute("http.cache.response.status", CacheResponseStatus.class);
   }
}
