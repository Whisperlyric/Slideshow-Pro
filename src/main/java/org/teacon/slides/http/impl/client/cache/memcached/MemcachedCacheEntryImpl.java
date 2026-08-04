package org.teacon.slides.http.impl.client.cache.memcached;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.teacon.slides.http.client.cache.HttpCacheEntry;

public class MemcachedCacheEntryImpl implements MemcachedCacheEntry {
   private String key;
   private HttpCacheEntry httpCacheEntry;

   public MemcachedCacheEntryImpl(String key, HttpCacheEntry httpCacheEntry) {
      this.key = key;
      this.httpCacheEntry = httpCacheEntry;
   }

   public MemcachedCacheEntryImpl() {
   }

   @Override
   public synchronized byte[] toByteArray() {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();

      try {
         ObjectOutputStream oos = new ObjectOutputStream(bos);
         oos.writeObject(this.key);
         oos.writeObject(this.httpCacheEntry);
         oos.close();
      } catch (IOException var4) {
         throw new MemcachedSerializationException(var4);
      }

      return bos.toByteArray();
   }

   @Override
   public synchronized String getStorageKey() {
      return this.key;
   }

   @Override
   public synchronized HttpCacheEntry getHttpCacheEntry() {
      return this.httpCacheEntry;
   }

   @Override
   public synchronized void set(byte[] bytes) {
      ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

      String s;
      HttpCacheEntry entry;
      try {
         ObjectInputStream ois = new ObjectInputStream(bis);
         s = (String)ois.readObject();
         entry = (HttpCacheEntry)ois.readObject();
         ois.close();
         bis.close();
      } catch (IOException var7) {
         throw new MemcachedSerializationException(var7);
      } catch (ClassNotFoundException var8) {
         throw new MemcachedSerializationException(var8);
      }

      this.key = s;
      this.httpCacheEntry = entry;
   }
}
