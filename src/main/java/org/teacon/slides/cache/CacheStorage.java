package org.teacon.slides.cache;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import net.minecraft.Util;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.message.BasicLineParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.Nullable;
import org.teacon.slides.Slideshow;
import org.teacon.slides.http.client.cache.HttpCacheEntry;
import org.teacon.slides.http.client.cache.HttpCacheStorage;
import org.teacon.slides.http.client.cache.HttpCacheUpdateCallback;
import org.teacon.slides.http.client.cache.Resource;
import org.teacon.slides.http.impl.client.cache.FileResource;

final class CacheStorage implements HttpCacheStorage {
   private static final Logger LOGGER = LogManager.getLogger(Slideshow.class);
   private static final Marker MARKER = MarkerManager.getMarker("Downloader");
   private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
   private final Object keyLock;
   private final Path parentPath;
   private final Path keyFilePath;
   private final AtomicInteger markedDirty = new AtomicInteger();
   private final Map<String, Pair<Path, HttpCacheEntry>> entries = new LinkedHashMap<>();
   private final ReferenceQueue<HttpCacheEntry> referenceQueue;
   private final Set<ResourceReference> resourceReferenceHolder;

   private static Pair<Path, HttpCacheEntry> normalize(Path parentPath, HttpCacheEntry entry) throws IOException {
      byte[] bytes = IOUtils.toByteArray(entry.getResource().getInputStream());
      Path tmp = Files.write(Files.createTempFile("slideshow-", ".tmp"), bytes);
      Path path = Files.move(tmp, parentPath.resolve(allocateImageName(bytes)), StandardCopyOption.REPLACE_EXISTING);
      return Pair.of(
         path,
         new HttpCacheEntry(
            entry.getRequestDate(),
            entry.getResponseDate(),
            entry.getStatusLine(),
            entry.getAllHeaders(),
            new FileResource(path.toFile()),
            entry.getVariantMap()
         )
      );
   }

   private static String allocateImageName(byte[] bytes) {
      MessageDigest sha1;
      try {
         sha1 = MessageDigest.getInstance("SHA-1");
      } catch (NoSuchAlgorithmException e) {
         throw new IllegalStateException("SHA-1 is not available", e);
      }

      String hashString = HexFormat.of().formatHex(sha1.digest(bytes));

      try {
         String var12;
         try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes)) {
            try (ImageInputStream imageStream = ImageIO.createImageInputStream(stream)) {
               Iterator<ImageReader> readers = ImageIO.getImageReaders(imageStream);
               if (readers.hasNext()) {
                  String[] suffixes = readers.next().getOriginatingProvider().getFileSuffixes();
                  if (suffixes.length > 0) {
                     return hashString + "." + suffixes[0].toLowerCase(Locale.ENGLISH);
                  }
               }
            }

            var12 = hashString;
         }

         return var12;
      } catch (IOException var11) {
         return hashString;
      }
   }

   private static void saveJson(Map<String, Pair<Path, HttpCacheEntry>> entries, JsonObject root) {
      for (Entry<String, Pair<Path, HttpCacheEntry>> entry : entries.entrySet()) {
         Path filePath = (Path)entry.getValue().getKey();
         HttpCacheEntry cacheEntry = (HttpCacheEntry)entry.getValue().getValue();
         root.add(entry.getKey(), (JsonElement)Util.make(new JsonObject(), child -> {
            child.addProperty("request_date", DateUtils.formatDate(cacheEntry.getRequestDate()));
            child.addProperty("response_date", DateUtils.formatDate(cacheEntry.getResponseDate()));
            child.addProperty("status_line", cacheEntry.getStatusLine().toString());
            child.add("headers", (JsonElement)Util.make(new JsonArray(), array -> {
               for (Header header : cacheEntry.getAllHeaders()) {
                  array.add(header.toString());
               }
            }));
            child.addProperty("resource", filePath.toString());
            child.add("variant_map", (JsonElement)Util.make(new JsonObject(), object -> {
               for (Entry<String, String> variantEntry : cacheEntry.getVariantMap().entrySet()) {
                  object.addProperty(variantEntry.getKey(), variantEntry.getValue());
               }
            }));
         }));
      }
   }

   private static void loadJson(Map<String, Pair<Path, HttpCacheEntry>> entries, JsonObject root) {
      for (Entry<String, JsonElement> entry : root.entrySet()) {
         JsonObject child = entry.getValue().getAsJsonObject();
         Date requestDate = DateUtils.parseDate(child.get("request_date").getAsString());
         Date responseDate = DateUtils.parseDate(child.get("response_date").getAsString());
         StatusLine statusLine = BasicLineParser.parseStatusLine(child.get("status_line").getAsString(), null);
         Path filePath = Paths.get(child.get("resource").getAsString());
         Header[] headers = loadHeaders(child);
         Map<String, String> variantMap = loadVariantMap(child);
         HttpCacheEntry cacheEntry = new HttpCacheEntry(requestDate, responseDate, statusLine, headers, new FileResource(filePath.toFile()), variantMap);
         entries.put(entry.getKey(), Pair.of(filePath, cacheEntry));
      }
   }

   private static Map<String, String> loadVariantMap(JsonObject child) {
      Builder<String, String> builder = ImmutableMap.builder();
      JsonObject map = child.has("variant_map") ? child.get("variant_map").getAsJsonObject() : new JsonObject();

      for (Entry<String, JsonElement> entry : map.entrySet()) {
         builder.put(entry.getKey(), entry.getValue().getAsString());
      }

      return builder.build();
   }

   private static Header[] loadHeaders(JsonObject child) {
      JsonArray list = child.has("headers") ? child.get("headers").getAsJsonArray() : new JsonArray();
      return Streams.stream(list).map(e -> BasicLineParser.parseHeader(e.getAsString(), null)).toArray(Header[]::new);
   }

   private void save() {
      JsonObject root = new JsonObject();
      synchronized (this.entries) {
         saveJson(this.entries, root);
      }

      synchronized (this.keyLock) {
         try (BufferedWriter writer = Files.newBufferedWriter(this.keyFilePath, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
         } catch (Exception var10) {
            LOGGER.warn(MARKER, "Failed to save cache storage. ", var10);
         }
      }
   }

   private void load() {
      JsonObject root = new JsonObject();
      synchronized (this.keyLock) {
         try (BufferedReader reader = Files.newBufferedReader(this.keyFilePath, StandardCharsets.UTF_8)) {
            root = (JsonObject)GSON.fromJson(reader, JsonObject.class);
         } catch (Exception var11) {
            LOGGER.warn(MARKER, "Failed to load cache storage. ", var11);
         }
      }

      synchronized (this.entries) {
         loadJson(this.entries, root);
      }
   }

   private void scheduleSave() {
      if (this.markedDirty.getAndIncrement() == 0) {
         Executor executor = CompletableFuture.delayedExecutor(5L, TimeUnit.SECONDS, Util.backgroundExecutor());
         CompletableFuture.runAsync(this::save, executor).thenRun(() -> {
            int changes = this.markedDirty.getAndSet(0);
            LOGGER.debug(MARKER, "Attempted to save {} change(s) to cache storage. ", changes);
         });
      }
   }

   public CacheStorage(Path parentPath) {
      this.keyLock = new Object();
      this.parentPath = parentPath;
      this.keyFilePath = this.parentPath.resolve("storage-keys.json");
      if (Files.exists(this.keyFilePath)) {
         this.load();
      } else if (LegacyStorage.loadLegacy(parentPath, this.entries)) {
         this.save();
      }

      this.referenceQueue = new ReferenceQueue<>();
      this.resourceReferenceHolder = Sets.newConcurrentHashSet();
   }

   private void keepResourceReference(HttpCacheEntry entry) {
      Resource resource = entry.getResource();
      if (resource != null) {
         ResourceReference ref = new ResourceReference(entry, this.referenceQueue);
         this.resourceReferenceHolder.add(ref);
      }
   }

   @Nullable
   public HttpCacheEntry getEntry(String url) {
      synchronized (this.entries) {
         Pair<Path, HttpCacheEntry> pair = this.entries.get(url);
         return pair != null ? (HttpCacheEntry)pair.getValue() : null;
      }
   }

   public void putEntry(String url, HttpCacheEntry entry) throws IOException {
      synchronized (this.entries) {
         Pair<Path, HttpCacheEntry> normalizedEntry = normalize(this.parentPath, entry);
         this.entries.put(url, normalizedEntry);
         this.keepResourceReference(entry);
      }

      this.scheduleSave();
   }

   public void removeEntry(String url) {
      synchronized (this.entries) {
         this.entries.remove(url);
      }

      this.scheduleSave();
   }

   public void updateEntry(String url, HttpCacheUpdateCallback cb) throws IOException {
      synchronized (this.entries) {
         Pair<Path, HttpCacheEntry> pair = this.entries.get(url);
         this.entries.put(url, normalize(this.parentPath, cb.update(pair != null ? (HttpCacheEntry)pair.getValue() : null)));
         HttpCacheEntry existing = (HttpCacheEntry)this.entries.get(url).getValue();
         HttpCacheEntry updated = cb.update(existing);
         if (existing != updated) {
            this.keepResourceReference(updated);
         }
      }

      this.scheduleSave();
   }

   public int cleanResources() {
      int prevCount = this.resourceReferenceHolder.size();

      ResourceReference ref;
      while ((ref = (ResourceReference)this.referenceQueue.poll()) != null) {
         this.resourceReferenceHolder.remove(ref);
         ref.getResource().dispose();
      }

      return prevCount - this.resourceReferenceHolder.size();
   }
}
