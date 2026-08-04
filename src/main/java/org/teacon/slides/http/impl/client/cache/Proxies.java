package org.teacon.slides.http.impl.client.cache;

import java.lang.reflect.Proxy;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.Args;

class Proxies {
   public static CloseableHttpResponse enhanceResponse(HttpResponse original) {
      Args.notNull(original, "HTTP response");
      return original instanceof CloseableHttpResponse
         ? (CloseableHttpResponse)original
         : (CloseableHttpResponse)Proxy.newProxyInstance(
            ResponseProxyHandler.class.getClassLoader(), new Class[]{CloseableHttpResponse.class}, new ResponseProxyHandler(original)
         );
   }
}
