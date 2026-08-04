package org.teacon.slides.http.impl.client.cache;

public interface FailureCache {
   int getErrorCount(String var1);

   void resetErrorCount(String var1);

   void increaseErrorCount(String var1);
}
