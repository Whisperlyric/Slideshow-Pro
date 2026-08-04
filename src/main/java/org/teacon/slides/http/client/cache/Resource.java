package org.teacon.slides.http.client.cache;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public interface Resource extends Serializable {
   InputStream getInputStream() throws IOException;

   long length();

   void dispose();
}
