package org.teacon.slides.http.impl.client.cache.memcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.MemcachedClientIF;
import net.spy.memcached.OperationTimeoutException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.teacon.slides.http.client.cache.HttpCacheEntry;
import org.teacon.slides.http.client.cache.HttpCacheEntrySerializer;
import org.teacon.slides.http.client.cache.HttpCacheStorage;
import org.teacon.slides.http.client.cache.HttpCacheUpdateCallback;
import org.teacon.slides.http.client.cache.HttpCacheUpdateException;
import org.teacon.slides.http.impl.client.cache.CacheConfig;

public class MemcachedHttpCacheStorage implements HttpCacheStorage {
   private static final Log log = LogFactory.getLog(MemcachedHttpCacheStorage.class);
   private final MemcachedClientIF client;
   private final KeyHashingScheme keyHashingScheme;
   private final MemcachedCacheEntryFactory memcachedCacheEntryFactory;
   private final int maxUpdateRetries;

   public MemcachedHttpCacheStorage(InetSocketAddress address) throws IOException {
      this(new MemcachedClient(new InetSocketAddress[]{address}));
   }

   public MemcachedHttpCacheStorage(MemcachedClientIF cache) {
      this(cache, CacheConfig.DEFAULT, new MemcachedCacheEntryFactoryImpl(), new SHA256KeyHashingScheme());
   }

   @Deprecated
   public MemcachedHttpCacheStorage(MemcachedClientIF client, CacheConfig config, HttpCacheEntrySerializer serializer) {
      this(client, config, new MemcachedCacheEntryFactoryImpl(), new SHA256KeyHashingScheme());
   }

   public MemcachedHttpCacheStorage(
      MemcachedClientIF client, CacheConfig config, MemcachedCacheEntryFactory memcachedCacheEntryFactory, KeyHashingScheme keyHashingScheme
   ) {
      this.client = client;
      this.maxUpdateRetries = config.getMaxUpdateRetries();
      this.memcachedCacheEntryFactory = memcachedCacheEntryFactory;
      this.keyHashingScheme = keyHashingScheme;
   }

   @Override
   public void putEntry(String url, HttpCacheEntry entry) throws IOException {
      byte[] bytes = this.serializeEntry(url, entry);
      String key = this.getCacheKey(url);
      if (key != null) {
         try {
            this.client.set(key, 0, bytes);
         } catch (OperationTimeoutException var6) {
            throw new MemcachedOperationTimeoutException(var6);
         }
      }
   }

   private String getCacheKey(String url) {
      try {
         return this.keyHashingScheme.hash(url);
      } catch (MemcachedKeyHashingException var3) {
         return null;
      }
   }

   private byte[] serializeEntry(String url, HttpCacheEntry hce) throws IOException {
      MemcachedCacheEntry mce = this.memcachedCacheEntryFactory.getMemcachedCacheEntry(url, hce);

      try {
         return mce.toByteArray();
      } catch (MemcachedSerializationException var6) {
         IOException ioe = new IOException();
         ioe.initCause(var6);
         throw ioe;
      }
   }

   private byte[] convertToByteArray(Object o) {
      if (o == null) {
         return null;
      } else if (!(o instanceof byte[])) {
         log.warn("got a non-bytearray back from memcached: " + o);
         return null;
      } else {
         return (byte[])o;
      }
   }

   private MemcachedCacheEntry reconstituteEntry(Object o) {
      byte[] bytes = this.convertToByteArray(o);
      if (bytes == null) {
         return null;
      } else {
         MemcachedCacheEntry mce = this.memcachedCacheEntryFactory.getUnsetCacheEntry();

         try {
            mce.set(bytes);
            return mce;
         } catch (MemcachedSerializationException var5) {
            return null;
         }
      }
   }

   @Override
   public HttpCacheEntry getEntry(String url) throws IOException {
      String key = this.getCacheKey(url);
      if (key == null) {
         return null;
      } else {
         try {
            MemcachedCacheEntry mce = this.reconstituteEntry(this.client.get(key));
            return mce != null && url.equals(mce.getStorageKey()) ? mce.getHttpCacheEntry() : null;
         } catch (OperationTimeoutException var4) {
            throw new MemcachedOperationTimeoutException(var4);
         }
      }
   }

   @Override
   public void removeEntry(String url) throws IOException {
      String key = this.getCacheKey(url);
      if (key != null) {
         try {
            this.client.delete(key);
         } catch (OperationTimeoutException var4) {
            throw new MemcachedOperationTimeoutException(var4);
         }
      }
   }

   @Override
   public void updateEntry(String url, HttpCacheUpdateCallback callback) throws HttpCacheUpdateException, IOException {
      int numRetries = 0;
      String key = this.getCacheKey(url);
      if (key == null) {
         throw new HttpCacheUpdateException("couldn't generate cache key");
      } else {
         do {
            try {
               CASValue<Object> v = this.client.gets(key);
               MemcachedCacheEntry mce = v == null ? null : this.reconstituteEntry(v.getValue());
               if (mce != null && !url.equals(mce.getStorageKey())) {
                  mce = null;
               }

               HttpCacheEntry existingEntry = mce == null ? null : mce.getHttpCacheEntry();
               HttpCacheEntry updatedEntry = callback.update(existingEntry);
               if (existingEntry == null) {
                  this.putEntry(url, updatedEntry);
                  return;
               }

               byte[] updatedBytes = this.serializeEntry(url, updatedEntry);
               CASResponse casResult = this.client.cas(key, v.getCas(), updatedBytes);
               if (casResult == CASResponse.OK) {
                  return;
               }

               numRetries++;
            } catch (OperationTimeoutException var11) {
               throw new MemcachedOperationTimeoutException(var11);
            }
         } while (numRetries <= this.maxUpdateRetries);

         throw new HttpCacheUpdateException("Failed to update");
      }
   }
}
