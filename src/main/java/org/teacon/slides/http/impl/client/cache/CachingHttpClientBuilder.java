package org.teacon.slides.http.impl.client.cache;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.execchain.ClientExecChain;
import org.teacon.slides.http.client.cache.HttpCacheInvalidator;
import org.teacon.slides.http.client.cache.HttpCacheStorage;
import org.teacon.slides.http.client.cache.ResourceFactory;

public class CachingHttpClientBuilder extends HttpClientBuilder {
   private ResourceFactory resourceFactory;
   private HttpCacheStorage storage;
   private File cacheDir;
   private CacheConfig cacheConfig;
   private SchedulingStrategy schedulingStrategy;
   private HttpCacheInvalidator httpCacheInvalidator;
   private boolean deleteCache = true;

   public static CachingHttpClientBuilder create() {
      return new CachingHttpClientBuilder();
   }

   protected CachingHttpClientBuilder() {
   }

   public final CachingHttpClientBuilder setResourceFactory(ResourceFactory resourceFactory) {
      this.resourceFactory = resourceFactory;
      return this;
   }

   public final CachingHttpClientBuilder setHttpCacheStorage(HttpCacheStorage storage) {
      this.storage = storage;
      return this;
   }

   public final CachingHttpClientBuilder setCacheDir(File cacheDir) {
      this.cacheDir = cacheDir;
      return this;
   }

   public final CachingHttpClientBuilder setCacheConfig(CacheConfig cacheConfig) {
      this.cacheConfig = cacheConfig;
      return this;
   }

   public final CachingHttpClientBuilder setSchedulingStrategy(SchedulingStrategy schedulingStrategy) {
      this.schedulingStrategy = schedulingStrategy;
      return this;
   }

   public final CachingHttpClientBuilder setHttpCacheInvalidator(HttpCacheInvalidator cacheInvalidator) {
      this.httpCacheInvalidator = cacheInvalidator;
      return this;
   }

   public CachingHttpClientBuilder setDeleteCache(boolean deleteCache) {
      this.deleteCache = deleteCache;
      return this;
   }

   protected ClientExecChain decorateMainExec(ClientExecChain mainExec) {
      CacheConfig config = this.cacheConfig != null ? this.cacheConfig : CacheConfig.DEFAULT;
      ResourceFactory resourceFactoryCopy = this.resourceFactory;
      if (resourceFactoryCopy == null) {
         if (this.cacheDir == null) {
            resourceFactoryCopy = new HeapResourceFactory();
         } else {
            resourceFactoryCopy = new FileResourceFactory(this.cacheDir);
         }
      }

      HttpCacheStorage storageCopy = this.storage;
      if (storageCopy == null) {
         if (this.cacheDir == null) {
            storageCopy = new BasicHttpCacheStorage(config);
         } else {
            final ManagedHttpCacheStorage managedStorage = new ManagedHttpCacheStorage(config);
            if (this.deleteCache) {
               this.addCloseable(new Closeable() {
                  @Override
                  public void close() throws IOException {
                     managedStorage.shutdown();
                  }
               });
            } else {
               this.addCloseable(managedStorage);
            }

            storageCopy = managedStorage;
         }
      }

      AsynchronousValidator revalidator = this.createAsynchronousRevalidator(config);
      CacheKeyGenerator uriExtractor = new CacheKeyGenerator();
      HttpCacheInvalidator cacheInvalidator = this.httpCacheInvalidator;
      if (cacheInvalidator == null) {
         cacheInvalidator = new CacheInvalidator(uriExtractor, storageCopy);
      }

      return new CachingExec(mainExec, new BasicHttpCache(resourceFactoryCopy, storageCopy, config, uriExtractor, cacheInvalidator), config, revalidator);
   }

   private AsynchronousValidator createAsynchronousRevalidator(CacheConfig config) {
      if (config.getAsynchronousWorkersMax() > 0) {
         SchedulingStrategy configuredSchedulingStrategy = this.createSchedulingStrategy(config);
         AsynchronousValidator revalidator = new AsynchronousValidator(configuredSchedulingStrategy);
         this.addCloseable(revalidator);
         return revalidator;
      } else {
         return null;
      }
   }

   private SchedulingStrategy createSchedulingStrategy(CacheConfig config) {
      return (SchedulingStrategy)(this.schedulingStrategy != null ? this.schedulingStrategy : new ImmediateSchedulingStrategy(config));
   }
}
