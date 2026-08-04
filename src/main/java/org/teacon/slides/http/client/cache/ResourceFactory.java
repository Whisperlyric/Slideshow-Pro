package org.teacon.slides.http.client.cache;

import java.io.IOException;
import java.io.InputStream;

public interface ResourceFactory {
   Resource generate(String var1, InputStream var2, InputLimit var3) throws IOException;

   Resource copy(String var1, Resource var2) throws IOException;
}
