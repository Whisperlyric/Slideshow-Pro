package org.teacon.slides.http.impl.client.cache;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicHttpResponse;
import org.teacon.slides.http.client.cache.InputLimit;
import org.teacon.slides.http.client.cache.Resource;
import org.teacon.slides.http.client.cache.ResourceFactory;

class SizeLimitedResponseReader {
   private final ResourceFactory resourceFactory;
   private final long maxResponseSizeBytes;
   private final HttpRequest request;
   private final CloseableHttpResponse response;
   private InputStream inStream;
   private InputLimit limit;
   private Resource resource;
   private boolean consumed;

   public SizeLimitedResponseReader(ResourceFactory resourceFactory, long maxResponseSizeBytes, HttpRequest request, CloseableHttpResponse response) {
      this.resourceFactory = resourceFactory;
      this.maxResponseSizeBytes = maxResponseSizeBytes;
      this.request = request;
      this.response = response;
   }

   protected void readResponse() throws IOException {
      if (!this.consumed) {
         this.doConsume();
      }
   }

   private void ensureNotConsumed() {
      if (this.consumed) {
         throw new IllegalStateException("Response has already been consumed");
      }
   }

   private void ensureConsumed() {
      if (!this.consumed) {
         throw new IllegalStateException("Response has not been consumed");
      }
   }

   private void doConsume() throws IOException {
      this.ensureNotConsumed();
      this.consumed = true;
      this.limit = new InputLimit(this.maxResponseSizeBytes);
      HttpEntity entity = this.response.getEntity();
      if (entity != null) {
         String uri = this.request.getRequestLine().getUri();
         this.inStream = entity.getContent();

         try {
            this.resource = this.resourceFactory.generate(uri, this.inStream, this.limit);
         } finally {
            if (!this.limit.isReached()) {
               this.inStream.close();
            }
         }
      }
   }

   boolean isLimitReached() {
      this.ensureConsumed();
      return this.limit.isReached();
   }

   Resource getResource() {
      this.ensureConsumed();
      return this.resource;
   }

   CloseableHttpResponse getReconstructedResponse() throws IOException {
      this.ensureConsumed();
      HttpResponse reconstructed = new BasicHttpResponse(this.response.getStatusLine());
      reconstructed.setHeaders(this.response.getAllHeaders());
      CombinedEntity combinedEntity = new CombinedEntity(this.resource, this.inStream);
      HttpEntity entity = this.response.getEntity();
      if (entity != null) {
         combinedEntity.setContentType(entity.getContentType());
         combinedEntity.setContentEncoding(entity.getContentEncoding());
         combinedEntity.setChunked(entity.isChunked());
      }

      reconstructed.setEntity(combinedEntity);
      return (CloseableHttpResponse)Proxy.newProxyInstance(
         ResponseProxyHandler.class.getClassLoader(), new Class[]{CloseableHttpResponse.class}, new ResponseProxyHandler(reconstructed) {
            @Override
            public void close() throws IOException {
               SizeLimitedResponseReader.this.response.close();
            }
         }
      );
   }
}
