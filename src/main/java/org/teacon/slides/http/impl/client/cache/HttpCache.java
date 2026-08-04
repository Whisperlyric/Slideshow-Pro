package org.teacon.slides.http.impl.client.cache;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.teacon.slides.http.client.cache.HttpCacheEntry;

interface HttpCache {
   void flushCacheEntriesFor(HttpHost var1, HttpRequest var2) throws IOException;

   void flushInvalidatedCacheEntriesFor(HttpHost var1, HttpRequest var2) throws IOException;

   void flushInvalidatedCacheEntriesFor(HttpHost var1, HttpRequest var2, HttpResponse var3);

   HttpCacheEntry getCacheEntry(HttpHost var1, HttpRequest var2) throws IOException;

   Map<String, Variant> getVariantCacheEntriesWithEtags(HttpHost var1, HttpRequest var2) throws IOException;

   HttpResponse cacheAndReturnResponse(HttpHost var1, HttpRequest var2, HttpResponse var3, Date var4, Date var5) throws IOException;

   CloseableHttpResponse cacheAndReturnResponse(HttpHost var1, HttpRequest var2, CloseableHttpResponse var3, Date var4, Date var5) throws IOException;

   HttpCacheEntry updateCacheEntry(HttpHost var1, HttpRequest var2, HttpCacheEntry var3, HttpResponse var4, Date var5, Date var6) throws IOException;

   HttpCacheEntry updateVariantCacheEntry(HttpHost var1, HttpRequest var2, HttpCacheEntry var3, HttpResponse var4, Date var5, Date var6, String var7) throws IOException;

   void reuseVariantEntryFor(HttpHost var1, HttpRequest var2, Variant var3) throws IOException;
}
