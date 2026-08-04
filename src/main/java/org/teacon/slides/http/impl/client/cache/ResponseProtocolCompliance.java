package org.teacon.slides.http.impl.client.cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.message.BasicHeader;

@Contract(
   threading = ThreadingBehavior.IMMUTABLE
)
class ResponseProtocolCompliance {
   private static final String UNEXPECTED_100_CONTINUE = "The incoming request did not contain a 100-continue header, but the response was a Status 100, continue.";
   private static final String UNEXPECTED_PARTIAL_CONTENT = "partial content was returned for a request that did not ask for it";

   public void ensureProtocolCompliance(HttpRequestWrapper request, HttpResponse response) throws IOException {
      if (this.backendResponseMustNotHaveBody(request, response)) {
         this.consumeBody(response);
         response.setEntity(null);
      }

      this.requestDidNotExpect100ContinueButResponseIsOne(request, response);
      this.transferEncodingIsNotReturnedTo1_0Client(request, response);
      this.ensurePartialContentIsNotSentToAClientThatDidNotRequestIt(request, response);
      this.ensure200ForOPTIONSRequestWithNoBodyHasContentLengthZero(request, response);
      this.ensure206ContainsDateHeader(response);
      this.ensure304DoesNotContainExtraEntityHeaders(response);
      this.identityIsNotUsedInContentEncoding(response);
      this.warningsWithNonMatchingWarnDatesAreRemoved(response);
   }

   private void consumeBody(HttpResponse response) throws IOException {
      HttpEntity body = response.getEntity();
      if (body != null) {
         IOUtils.consume(body);
      }
   }

   private void warningsWithNonMatchingWarnDatesAreRemoved(HttpResponse response) {
      Date responseDate = DateUtils.parseDate(response.getFirstHeader("Date").getValue());
      if (responseDate != null) {
         Header[] warningHeaders = response.getHeaders("Warning");
         if (warningHeaders != null && warningHeaders.length != 0) {
            List<Header> newWarningHeaders = new ArrayList<>();
            boolean modified = false;

            for (Header h : warningHeaders) {
               for (WarningValue wv : WarningValue.getWarningValues(h)) {
                  Date warnDate = wv.getWarnDate();
                  if (warnDate != null && !warnDate.equals(responseDate)) {
                     modified = true;
                  } else {
                     newWarningHeaders.add(new BasicHeader("Warning", wv.toString()));
                  }
               }
            }

            if (modified) {
               response.removeHeaders("Warning");

               for (Header h : newWarningHeaders) {
                  response.addHeader(h);
               }
            }
         }
      }
   }

   private void identityIsNotUsedInContentEncoding(HttpResponse response) {
      Header[] hdrs = response.getHeaders("Content-Encoding");
      if (hdrs != null && hdrs.length != 0) {
         List<Header> newHeaders = new ArrayList<>();
         boolean modified = false;

         for (Header h : hdrs) {
            StringBuilder buf = new StringBuilder();
            boolean first = true;

            for (HeaderElement elt : h.getElements()) {
               if ("identity".equalsIgnoreCase(elt.getName())) {
                  modified = true;
               } else {
                  if (!first) {
                     buf.append(",");
                  }

                  buf.append(elt.toString());
                  first = false;
               }
            }

            String newHeaderValue = buf.toString();
            if (!"".equals(newHeaderValue)) {
               newHeaders.add(new BasicHeader("Content-Encoding", newHeaderValue));
            }
         }

         if (modified) {
            response.removeHeaders("Content-Encoding");

            for (Header h : newHeaders) {
               response.addHeader(h);
            }
         }
      }
   }

   private void ensure206ContainsDateHeader(HttpResponse response) {
      if (response.getFirstHeader("Date") == null) {
         response.addHeader("Date", DateUtils.formatDate(new Date()));
      }
   }

   private void ensurePartialContentIsNotSentToAClientThatDidNotRequestIt(HttpRequest request, HttpResponse response) throws IOException {
      if (request.getFirstHeader("Range") == null && response.getStatusLine().getStatusCode() == 206) {
         this.consumeBody(response);
         throw new ClientProtocolException("partial content was returned for a request that did not ask for it");
      }
   }

   private void ensure200ForOPTIONSRequestWithNoBodyHasContentLengthZero(HttpRequest request, HttpResponse response) {
      if (request.getRequestLine().getMethod().equalsIgnoreCase("OPTIONS")) {
         if (response.getStatusLine().getStatusCode() == 200) {
            if (response.getFirstHeader("Content-Length") == null) {
               response.addHeader("Content-Length", "0");
            }
         }
      }
   }

   private void ensure304DoesNotContainExtraEntityHeaders(HttpResponse response) {
      String[] disallowedEntityHeaders = new String[]{
         "Allow", "Content-Encoding", "Content-Language", "Content-Length", "Content-MD5", "Content-Range", "Content-Type", "Last-Modified"
      };
      if (response.getStatusLine().getStatusCode() == 304) {
         for (String hdr : disallowedEntityHeaders) {
            response.removeHeaders(hdr);
         }
      }
   }

   private boolean backendResponseMustNotHaveBody(HttpRequest request, HttpResponse backendResponse) {
      return "HEAD".equals(request.getRequestLine().getMethod())
         || backendResponse.getStatusLine().getStatusCode() == 204
         || backendResponse.getStatusLine().getStatusCode() == 205
         || backendResponse.getStatusLine().getStatusCode() == 304;
   }

   private void requestDidNotExpect100ContinueButResponseIsOne(HttpRequestWrapper request, HttpResponse response) throws IOException {
      if (response.getStatusLine().getStatusCode() == 100) {
         HttpRequest originalRequest = request.getOriginal();
         if (!(originalRequest instanceof HttpEntityEnclosingRequest) || !((HttpEntityEnclosingRequest)originalRequest).expectContinue()) {
            this.consumeBody(response);
            throw new ClientProtocolException("The incoming request did not contain a 100-continue header, but the response was a Status 100, continue.");
         }
      }
   }

   private void transferEncodingIsNotReturnedTo1_0Client(HttpRequestWrapper request, HttpResponse response) {
      HttpRequest originalRequest = request.getOriginal();
      if (originalRequest.getProtocolVersion().compareToVersion(HttpVersion.HTTP_1_1) < 0) {
         this.removeResponseTransferEncoding(response);
      }
   }

   private void removeResponseTransferEncoding(HttpResponse response) {
      response.removeHeaders("TE");
      response.removeHeaders("Transfer-Encoding");
   }
}
