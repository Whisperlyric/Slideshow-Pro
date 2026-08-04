package org.teacon.slides.http.impl.client.cache;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.client.utils.DateUtils;

@Contract(
   threading = ThreadingBehavior.IMMUTABLE
)
class ResponseCachingPolicy {
   private static final String[] AUTH_CACHEABLE_PARAMS = new String[]{"s-maxage", "must-revalidate", "public"};
   private final long maxObjectSizeBytes;
   private final boolean sharedCache;
   private final boolean neverCache1_0ResponsesWithQueryString;
   private final Log log = LogFactory.getLog(this.getClass());
   private static final Set<Integer> cacheableStatuses = new HashSet<>(Arrays.asList(200, 203, 300, 301, 410));
   private final Set<Integer> uncacheableStatuses;

   public ResponseCachingPolicy(long maxObjectSizeBytes, boolean sharedCache, boolean neverCache1_0ResponsesWithQueryString, boolean allow303Caching) {
      this.maxObjectSizeBytes = maxObjectSizeBytes;
      this.sharedCache = sharedCache;
      this.neverCache1_0ResponsesWithQueryString = neverCache1_0ResponsesWithQueryString;
      if (allow303Caching) {
         this.uncacheableStatuses = new HashSet<>(Arrays.asList(206));
      } else {
         this.uncacheableStatuses = new HashSet<>(Arrays.asList(206, 303));
      }
   }

   public boolean isResponseCacheable(String httpMethod, HttpResponse response) {
      boolean cacheable = false;
      if (!"GET".equals(httpMethod) && !"HEAD".equals(httpMethod)) {
         this.log.debug("Response was not cacheable.");
         return false;
      } else {
         int status = response.getStatusLine().getStatusCode();
         if (cacheableStatuses.contains(status)) {
            cacheable = true;
         } else {
            if (this.uncacheableStatuses.contains(status)) {
               return false;
            }

            if (this.unknownStatusCode(status)) {
               return false;
            }
         }

         Header contentLength = response.getFirstHeader("Content-Length");
         if (contentLength != null) {
            long contentLengthValue = Long.parseLong(contentLength.getValue());
            if (contentLengthValue > this.maxObjectSizeBytes) {
               return false;
            }
         }

         Header[] ageHeaders = response.getHeaders("Age");
         if (ageHeaders.length > 1) {
            return false;
         } else {
            Header[] expiresHeaders = response.getHeaders("Expires");
            if (expiresHeaders.length > 1) {
               return false;
            } else {
               Header[] dateHeaders = response.getHeaders("Date");
               if (dateHeaders.length != 1) {
                  return false;
               } else {
                  Date date = DateUtils.parseDate(dateHeaders[0].getValue());
                  if (date == null) {
                     return false;
                  } else {
                     for (Header varyHdr : response.getHeaders("Vary")) {
                        for (HeaderElement elem : varyHdr.getElements()) {
                           if ("*".equals(elem.getName())) {
                              return false;
                           }
                        }
                     }

                     return this.isExplicitlyNonCacheable(response) ? false : cacheable || this.isExplicitlyCacheable(response);
                  }
               }
            }
         }
      }
   }

   private boolean unknownStatusCode(int status) {
      if (status >= 100 && status <= 101) {
         return false;
      } else if (status >= 200 && status <= 206) {
         return false;
      } else if (status >= 300 && status <= 307) {
         return false;
      } else {
         return status >= 400 && status <= 417 ? false : status < 500 || status > 505;
      }
   }

   protected boolean isExplicitlyNonCacheable(HttpResponse response) {
      Header[] cacheControlHeaders = response.getHeaders("Cache-Control");

      for (Header header : cacheControlHeaders) {
         for (HeaderElement elem : header.getElements()) {
            if ("no-store".equals(elem.getName()) || "no-cache".equals(elem.getName()) || this.sharedCache && "private".equals(elem.getName())) {
               return true;
            }
         }
      }

      return false;
   }

   protected boolean hasCacheControlParameterFrom(HttpMessage msg, String[] params) {
      Header[] cacheControlHeaders = msg.getHeaders("Cache-Control");

      for (Header header : cacheControlHeaders) {
         for (HeaderElement elem : header.getElements()) {
            for (String param : params) {
               if (param.equalsIgnoreCase(elem.getName())) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   protected boolean isExplicitlyCacheable(HttpResponse response) {
      if (response.getFirstHeader("Expires") != null) {
         return true;
      } else {
         String[] cacheableParams = new String[]{"max-age", "s-maxage", "must-revalidate", "proxy-revalidate", "public"};
         return this.hasCacheControlParameterFrom(response, cacheableParams);
      }
   }

   public boolean isResponseCacheable(HttpRequest request, HttpResponse response) {
      if (this.requestProtocolGreaterThanAccepted(request)) {
         this.log.debug("Response was not cacheable.");
         return false;
      } else {
         String[] uncacheableRequestDirectives = new String[]{"no-store"};
         if (this.hasCacheControlParameterFrom(request, uncacheableRequestDirectives)) {
            return false;
         } else {
            if (request.getRequestLine().getUri().contains("?")) {
               if (this.neverCache1_0ResponsesWithQueryString && this.from1_0Origin(response)) {
                  this.log.debug("Response was not cacheable as it had a query string.");
                  return false;
               }

               if (!this.isExplicitlyCacheable(response)) {
                  this.log.debug("Response was not cacheable as it is missing explicit caching headers.");
                  return false;
               }
            }

            if (this.expiresHeaderLessOrEqualToDateHeaderAndNoCacheControl(response)) {
               return false;
            } else {
               if (this.sharedCache) {
                  Header[] authNHeaders = request.getHeaders("Authorization");
                  if (authNHeaders != null && authNHeaders.length > 0 && !this.hasCacheControlParameterFrom(response, AUTH_CACHEABLE_PARAMS)) {
                     return false;
                  }
               }

               String method = request.getRequestLine().getMethod();
               return this.isResponseCacheable(method, response);
            }
         }
      }
   }

   private boolean expiresHeaderLessOrEqualToDateHeaderAndNoCacheControl(HttpResponse response) {
      if (response.getFirstHeader("Cache-Control") != null) {
         return false;
      } else {
         Header expiresHdr = response.getFirstHeader("Expires");
         Header dateHdr = response.getFirstHeader("Date");
         if (expiresHdr != null && dateHdr != null) {
            Date expires = DateUtils.parseDate(expiresHdr.getValue());
            Date date = DateUtils.parseDate(dateHdr.getValue());
            return expires != null && date != null ? expires.equals(date) || expires.before(date) : false;
         } else {
            return false;
         }
      }
   }

   private boolean from1_0Origin(HttpResponse response) {
      Header via = response.getFirstHeader("Via");
      if (via != null) {
         HeaderElement[] arr$ = via.getElements();
         int len$ = arr$.length;
         int i$ = 0;
         if (i$ < len$) {
            HeaderElement elt = arr$[i$];
            String proto = elt.toString().split("\\s")[0];
            if (proto.contains("/")) {
               return proto.equals("HTTP/1.0");
            }

            return proto.equals("1.0");
         }
      }

      return HttpVersion.HTTP_1_0.equals(response.getProtocolVersion());
   }

   private boolean requestProtocolGreaterThanAccepted(HttpRequest req) {
      return req.getProtocolVersion().compareToVersion(HttpVersion.HTTP_1_1) > 0;
   }
}
