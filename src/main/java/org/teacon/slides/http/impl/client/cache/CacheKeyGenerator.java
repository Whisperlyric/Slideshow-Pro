package org.teacon.slides.http.impl.client.cache;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.util.Args;
import org.teacon.slides.http.client.cache.HttpCacheEntry;

@Contract(
   threading = ThreadingBehavior.IMMUTABLE
)
class CacheKeyGenerator {
   private static final URI BASE_URI = URI.create("http://example.com/");

   static URIBuilder getRequestUriBuilder(HttpRequest request) throws URISyntaxException {
      if (request instanceof HttpUriRequest) {
         URI uri = ((HttpUriRequest)request).getURI();
         if (uri != null) {
            return new URIBuilder(uri);
         }
      }

      return new URIBuilder(request.getRequestLine().getUri());
   }

   static URI getRequestUri(HttpRequest request, HttpHost target) throws URISyntaxException {
      Args.notNull(request, "HTTP request");
      Args.notNull(target, "Target");
      URIBuilder uriBuilder = getRequestUriBuilder(request);
      String path = uriBuilder.getPath();
      if (path != null) {
         uriBuilder.setPathSegments(URLEncodedUtils.parsePathSegments(path));
      }

      if (!uriBuilder.isAbsolute()) {
         uriBuilder.setScheme(target.getSchemeName());
         uriBuilder.setHost(target.getHostName());
         uriBuilder.setPort(target.getPort());
      }

      return uriBuilder.build();
   }

   static URI normalize(URI requestUri) throws URISyntaxException {
      Args.notNull(requestUri, "URI");
      URIBuilder builder = new URIBuilder(requestUri.isAbsolute() ? URIUtils.resolve(BASE_URI, requestUri) : requestUri);
      if (builder.getHost() != null) {
         if (builder.getScheme() == null) {
            builder.setScheme("http");
         }

         if (builder.getPort() <= -1) {
            if ("http".equalsIgnoreCase(builder.getScheme())) {
               builder.setPort(80);
            } else if ("https".equalsIgnoreCase(builder.getScheme())) {
               builder.setPort(443);
            }
         }
      }

      builder.setFragment(null);
      return builder.build();
   }

   public String getURI(HttpHost host, HttpRequest req) {
      try {
         URI uri = normalize(getRequestUri(req, host));
         return uri.toASCIIString();
      } catch (URISyntaxException var4) {
         return req.getRequestLine().getUri();
      }
   }

   public String canonicalizeUri(String uri) {
      try {
         URI normalized = normalize(URIUtils.resolve(BASE_URI, uri));
         return normalized.toASCIIString();
      } catch (URISyntaxException var3) {
         return uri;
      }
   }

   protected String getFullHeaderValue(Header[] headers) {
      if (headers == null) {
         return "";
      } else {
         StringBuilder buf = new StringBuilder("");
         boolean first = true;

         for (Header hdr : headers) {
            if (!first) {
               buf.append(", ");
            }

            buf.append(hdr.getValue().trim());
            first = false;
         }

         return buf.toString();
      }
   }

   public String getVariantURI(HttpHost host, HttpRequest req, HttpCacheEntry entry) {
      return !entry.hasVariants() ? this.getURI(host, req) : this.getVariantKey(req, entry) + this.getURI(host, req);
   }

   public String getVariantKey(HttpRequest req, HttpCacheEntry entry) {
      List<String> variantHeaderNames = new ArrayList<>();

      for (Header varyHdr : entry.getHeaders("Vary")) {
         for (HeaderElement elt : varyHdr.getElements()) {
            variantHeaderNames.add(elt.getName());
         }
      }

      Collections.sort(variantHeaderNames);

      StringBuilder buf;
      try {
         buf = new StringBuilder("{");
         boolean first = true;

         for (String headerName : variantHeaderNames) {
            if (!first) {
               buf.append("&");
            }

            buf.append(URLEncoder.encode(headerName, Consts.UTF_8.name()));
            buf.append("=");
            buf.append(URLEncoder.encode(this.getFullHeaderValue(req.getHeaders(headerName)), Consts.UTF_8.name()));
            first = false;
         }

         buf.append("}");
      } catch (UnsupportedEncodingException var12) {
         throw new RuntimeException("couldn't encode to UTF-8", var12);
      }

      return buf.toString();
   }
}
