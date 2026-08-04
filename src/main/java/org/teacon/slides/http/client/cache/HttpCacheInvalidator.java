package org.teacon.slides.http.client.cache;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

public interface HttpCacheInvalidator {
   void flushInvalidatedCacheEntries(HttpHost var1, HttpRequest var2);

   void flushInvalidatedCacheEntries(HttpHost var1, HttpRequest var2, HttpResponse var3);
}
