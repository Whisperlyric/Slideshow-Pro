package org.teacon.slides.http.impl.client.cache.memcached;

public class PrefixKeyHashingScheme implements KeyHashingScheme {
   private final String prefix;
   private final KeyHashingScheme backingScheme;

   public PrefixKeyHashingScheme(String prefix, KeyHashingScheme backingScheme) {
      this.prefix = prefix;
      this.backingScheme = backingScheme;
   }

   @Override
   public String hash(String storageKey) {
      return this.prefix + this.backingScheme.hash(storageKey);
   }
}
