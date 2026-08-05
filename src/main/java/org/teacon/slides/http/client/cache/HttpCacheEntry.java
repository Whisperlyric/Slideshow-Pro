package org.teacon.slides.http.client.cache;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.message.HeaderGroup;
import org.apache.http.util.Args;

@Contract(
   threading = ThreadingBehavior.IMMUTABLE
)
public class HttpCacheEntry implements Serializable {
   @Serial
   private static final long serialVersionUID = -6300496422359477413L;
   private static final String REQUEST_METHOD_HEADER_NAME = "Hc-Request-Method";
   private final Date requestDate;
   private final Date responseDate;
   private final StatusLine statusLine;
   private final HeaderGroup responseHeaders;
   private final Resource resource;
   private final Map<String, String> variantMap;
   private final Date date;

   public HttpCacheEntry(
      Date requestDate,
      Date responseDate,
      StatusLine statusLine,
      Header[] responseHeaders,
      Resource resource,
      Map<String, String> variantMap,
      String requestMethod
   ) {
      Args.notNull(requestDate, "Request date");
      Args.notNull(responseDate, "Response date");
      Args.notNull(statusLine, "Status line");
      Args.notNull(responseHeaders, "Response headers");
      this.requestDate = requestDate;
      this.responseDate = responseDate;
      this.statusLine = statusLine;
      this.responseHeaders = new HeaderGroup();
      this.responseHeaders.setHeaders(responseHeaders);
      this.resource = resource;
      this.variantMap = variantMap != null ? new HashMap<>(variantMap) : null;
      this.date = this.parseDate();
   }

   public HttpCacheEntry(
      Date requestDate, Date responseDate, StatusLine statusLine, Header[] responseHeaders, Resource resource, Map<String, String> variantMap
   ) {
      this(requestDate, responseDate, statusLine, responseHeaders, resource, variantMap, null);
   }

   public HttpCacheEntry(Date requestDate, Date responseDate, StatusLine statusLine, Header[] responseHeaders, Resource resource) {
      this(requestDate, responseDate, statusLine, responseHeaders, resource, new HashMap<>());
   }

   public HttpCacheEntry(Date requestDate, Date responseDate, StatusLine statusLine, Header[] responseHeaders, Resource resource, String requestMethod) {
      this(requestDate, responseDate, statusLine, responseHeaders, resource, new HashMap<>(), requestMethod);
   }

   private Date parseDate() {
      Header dateHdr = this.getFirstHeader("Date");
      return dateHdr == null ? null : DateUtils.parseDate(dateHdr.getValue());
   }

   public StatusLine getStatusLine() {
      return this.statusLine;
   }

   public ProtocolVersion getProtocolVersion() {
      return this.statusLine.getProtocolVersion();
   }

   public String getReasonPhrase() {
      return this.statusLine.getReasonPhrase();
   }

   public int getStatusCode() {
      return this.statusLine.getStatusCode();
   }

   public Date getRequestDate() {
      return this.requestDate;
   }

   public Date getResponseDate() {
      return this.responseDate;
   }

   public Header[] getAllHeaders() {
      HeaderGroup filteredHeaders = new HeaderGroup();
      HeaderIterator iterator = this.responseHeaders.iterator();

      while (iterator.hasNext()) {
         Header header = (Header)iterator.next();
         if (!"Hc-Request-Method".equals(header.getName())) {
            filteredHeaders.addHeader(header);
         }
      }

      return filteredHeaders.getAllHeaders();
   }

   public Header getFirstHeader(String name) {
      return "Hc-Request-Method".equalsIgnoreCase(name) ? null : this.responseHeaders.getFirstHeader(name);
   }

   public Header[] getHeaders(String name) {
      return "Hc-Request-Method".equalsIgnoreCase(name) ? new Header[0] : this.responseHeaders.getHeaders(name);
   }

   public Date getDate() {
      return this.date;
   }

   public Resource getResource() {
      return this.resource;
   }

   public boolean hasVariants() {
      return this.getFirstHeader("Vary") != null;
   }

   public Map<String, String> getVariantMap() {
      return Collections.unmodifiableMap(this.variantMap);
   }

   public String getRequestMethod() {
      Header requestMethodHeader = this.responseHeaders.getFirstHeader("Hc-Request-Method");
      return requestMethodHeader != null ? requestMethodHeader.getValue() : "GET";
   }

   @Override
   public String toString() {
      return "[request date=" + this.requestDate + "; response date=" + this.responseDate + "; statusLine=" + this.statusLine + "]";
   }
}
