package org.teacon.slides.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import net.minecraft.client.Minecraft;
import org.apache.http.HttpHost;
import org.teacon.slides.Slideshow;
import org.teacon.slides.util.Utilities;

public final class Config {
   private static boolean proxySwitch = false;
   private static String host = "localhost";
   private static int port = 8080;
   private static int renderDistance = 256;
   private static HttpHost PROXY;
   private static final String PROXY_SWITCH = "proxySwitch";
   private static final String HOST = "host";
   private static final String PORT = "port";
   private static final String VIEW_DISTANCE = "slideshowViewDistance";
   private static final Path CONFIG_PATH = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("slideshow.json");

   public static boolean isProxySwitch() {
      return proxySwitch;
   }

   public static int getRenderDistance() {
      return renderDistance;
   }

   public static HttpHost getPROXY() {
      return PROXY;
   }

   public static void refreshProperties() {
      Slideshow.LOGGER.info("Refreshed Slideshow client config");

      try {
         JsonObject jsonConfig = JsonParser.parseString(String.join("", Files.readAllLines(CONFIG_PATH))).getAsJsonObject();

         try {
            proxySwitch = jsonConfig.get("proxySwitch").getAsBoolean();
         } catch (Exception var5) {
         }

         try {
            host = jsonConfig.get("host").getAsString();
         } catch (Exception var4) {
         }

         try {
            port = jsonConfig.get("port").getAsInt();
         } catch (Exception var3) {
         }

         try {
            renderDistance = jsonConfig.get("slideshowViewDistance").getAsInt();
         } catch (Exception var2) {
         }

         if (proxySwitch) {
            PROXY = new HttpHost(host, port);
            Slideshow.LOGGER.info("Proxy loaded");
            Slideshow.LOGGER.info("host: {}", host);
            Slideshow.LOGGER.info("port: {}", port);
         } else {
            PROXY = null;
         }
      } catch (Exception var6) {
         writeToFile();
         refreshProperties();
      }
   }

   private static void writeToFile() {
      Slideshow.LOGGER.info("Wrote Slideshow client config to file");
      JsonObject jsonConfig = new JsonObject();
      jsonConfig.addProperty("proxySwitch", proxySwitch);
      jsonConfig.addProperty("host", host);
      jsonConfig.addProperty("port", port);
      jsonConfig.addProperty("slideshowViewDistance", renderDistance);

      try {
         if (!Files.exists(CONFIG_PATH.getParent())) {
            Files.createDirectories(CONFIG_PATH.getParent());
         }

         Files.write(CONFIG_PATH, Collections.singleton(Utilities.prettyPrint(jsonConfig)));
      } catch (IOException var2) {
         Slideshow.LOGGER.error("Configuration file write exception", var2);
      }
   }
}
