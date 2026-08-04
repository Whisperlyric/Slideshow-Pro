package org.teacon.slides.http.impl.client.cache;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.http.HttpResponse;

class ResponseProxyHandler implements InvocationHandler {
   private static final Method CLOSE_METHOD;
   private final HttpResponse original;

   ResponseProxyHandler(HttpResponse original) {
      this.original = original;
   }

   public void close() throws IOException {
      IOUtils.consume(this.original.getEntity());
   }

   @Override
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.equals(CLOSE_METHOD)) {
         this.close();
         return null;
      } else {
         try {
            return method.invoke(this.original, args);
         } catch (InvocationTargetException var6) {
            Throwable cause = var6.getCause();
            if (cause != null) {
               throw cause;
            } else {
               throw var6;
            }
         }
      }
   }

   static {
      try {
         CLOSE_METHOD = Closeable.class.getMethod("close");
      } catch (NoSuchMethodException var1) {
         throw new Error(var1);
      }
   }
}
