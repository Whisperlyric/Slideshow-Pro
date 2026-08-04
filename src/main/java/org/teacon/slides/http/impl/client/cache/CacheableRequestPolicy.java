package org.teacon.slides.http.impl.client.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;

@Contract(
   threading = ThreadingBehavior.IMMUTABLE
)
class CacheableRequestPolicy {
   private final Log log = LogFactory.getLog(this.getClass());

   public boolean isServableFromCache(HttpRequest request) {
      String method = request.getRequestLine().getMethod();
      ProtocolVersion pv = request.getRequestLine().getProtocolVersion();
      if (HttpVersion.HTTP_1_1.compareToVersion(pv) != 0) {
         this.log.trace("non-HTTP/1.1 request was not serveable from cache");
         return false;
      } else if (!method.equals("GET") && !method.equals("HEAD")) {
         this.log.trace("non-GET or non-HEAD request was not serveable from cache");
         return false;
      } else if (request.getHeaders("Pragma").length > 0) {
         this.log.trace("request with Pragma header was not serveable from cache");
         return false;
      } else {
         Header[] cacheControlHeaders = request.getHeaders("Cache-Control");

         for (Header cacheControl : cacheControlHeaders) {
            for (HeaderElement cacheControlElement : cacheControl.getElements()) {
               if ("no-store".equalsIgnoreCase(cacheControlElement.getName())) {
                  this.log.trace("Request with no-store was not serveable from cache");
                  return false;
               }

               if ("no-cache".equalsIgnoreCase(cacheControlElement.getName())) {
                  this.log.trace("Request with no-cache was not serveable from cache");
                  return false;
               }
            }
         }

         this.log.trace("Request was serveable from cache");
         return true;
      }
   }
}
