package org.teacon.slides.cache;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.NotNull;
import org.teacon.slides.Slideshow;
import org.teacon.slides.config.Config;
import org.teacon.slides.http.client.cache.HttpCacheContext;
import org.teacon.slides.http.impl.client.cache.CacheConfig;
import org.teacon.slides.http.impl.client.cache.CachingHttpClients;

public final class ImageCache {
   private static final Logger LOGGER = LogManager.getLogger(Slideshow.class);
   private static final Marker MARKER = MarkerManager.getMarker("Cache");
   private static final Path LOCAL_CACHE_PATH = Paths.get("slideshow");
   private static volatile ImageCache sInstance;
   private static final int MAX_CACHE_OBJECT_SIZE = 536870912;
   private static final CacheConfig CONFIG = CacheConfig.custom().setMaxObjectSize(536870912L).setSharedCache(false).build();
   private static final String DEFAULT_REFERER = "https://github.com/jonafanho/Slideshow";
   private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11";
   private final CloseableHttpClient mHttpClient;
   private final CacheStorage mCacheStorage;

   public static ImageCache getInstance() {
      if (sInstance != null) {
         return sInstance;
      } else {
         synchronized (ImageCache.class) {
            if (sInstance == null) {
               sInstance = new ImageCache(LOCAL_CACHE_PATH);
            }
         }

         return sInstance;
      }
   }

   private ImageCache(Path dir) {
      try {
         Files.createDirectories(dir);
      } catch (IOException var3) {
         throw new RuntimeException("Failed to create cache directory for slide images.", var3);
      }

      this.mCacheStorage = new CacheStorage(dir);
      if (Config.isProxySwitch()) {
         this.mHttpClient = CachingHttpClients.custom().setCacheConfig(CONFIG).setHttpCacheStorage(this.mCacheStorage).setProxy(Config.getPROXY()).build();
      } else {
         this.mHttpClient = CachingHttpClients.custom().setCacheConfig(CONFIG).setHttpCacheStorage(this.mCacheStorage).build();
      }
   }

   @NotNull
   public CompletableFuture<byte[]> getResource(@NotNull URI location, boolean online) {
      return CompletableFuture.supplyAsync(() -> {
         HttpCacheContext context = HttpCacheContext.create();

         try {
            CloseableHttpResponse response = this.createResponse(location, context, online);

            byte[] e;
            try {
               try {
                  e = IOUtils.toByteArray(response.getEntity().getContent());
               } catch (IOException var8) {
                  if (online) {
                     LOGGER.warn(MARKER, "Failed to read bytes from remote source.", var8);
                  }

                  throw new CompletionException(var8);
               }
            } catch (Throwable var9) {
               if (response != null) {
                  try {
                     response.close();
                  } catch (Throwable var7) {
                     var9.addSuppressed(var7);
                  }
               }

               throw var9;
            }

            if (response != null) {
               response.close();
            }

            return e;
         } catch (ClientProtocolException var10) {
            LOGGER.warn(MARKER, "Detected invalid client protocol.", var10);
            throw new CompletionException(var10);
         } catch (IOException var11) {
            LOGGER.warn(MARKER, "Failed to establish connection.", var11);
            throw new CompletionException(var11);
         }
      });
   }

   @NotNull
   public CompletableFuture<byte[]> getResourceFromPack(@NotNull ResourceLocation location) {
      return CompletableFuture.supplyAsync(() -> {
         Minecraft mc = Minecraft.getInstance();

         try {
            return IOUtils.toByteArray(mc.getResourceManager().open(location));
         } catch (IOException var3) {
            LOGGER.warn(MARKER, "Failed to read bytes from resource pack.", var3);
            throw new CompletionException(var3);
         }
      });
   }

   private CloseableHttpResponse createResponse(URI location, HttpCacheContext context, boolean online) throws IOException {
      HttpGet request = new HttpGet(location);
      request.addHeader("Referer", "https://github.com/jonafanho/Slideshow");
      request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
      request.addHeader("Accept", String.join(", ", ImageIO.getReaderMIMETypes()));
      if (!online) {
         request.addHeader("Cache-Control", "max-stale=2147483647");
         request.addHeader("Cache-Control", "only-if-cached");
      } else {
         request.addHeader("Cache-Control", "must-revalidate");
      }

      return this.mHttpClient.execute(request, context);
   }

   private void logRequestHeader(@NotNull HttpCacheContext context) {
      LOGGER.debug(MARKER, " >> {}", context.getRequest().getRequestLine());

      for (Header header : context.getRequest().getAllHeaders()) {
         LOGGER.debug(MARKER, " >> {}", header);
      }

      LOGGER.debug(MARKER, " << {}", context.getResponse().getStatusLine());

      for (Header header : context.getResponse().getAllHeaders()) {
         LOGGER.debug(MARKER, " << {}", header);
      }

      LOGGER.debug(MARKER, "Remote server status: {}", context.getCacheResponseStatus());
   }

   public int cleanResources() {
      return this.mCacheStorage.cleanResources();
   }
}
