package org.teacon.slides.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.teacon.slides.Slideshow;
import org.teacon.slides.util.Utilities;

public class ServerConfig {
   private static boolean initialized = false;
   private static Path CONFIG_PATH;
   private static int projectorPermission = 2;
   private static boolean projectorRequiresCreative = true;
   private static int commandsPermission = 2;
   private static final String PROJECTOR_PERMISSION = "projectorPermission";
   private static final String PROJECTOR_REQUIRES_CREATIVE = "projectorRequiresCreative";
   private static final String COMMANDS_PERMISSION = "commandsPermission";

   public static void init(MinecraftServer server) {
      if (!initialized) {
         try {
            CONFIG_PATH = server.getWorldPath(LevelResource.ROOT).resolve("config").resolve("slideshow.json");
            initialized = true;
         } catch (Exception var2) {
         }
      }
   }

   public static void uninit() {
      initialized = false;
      projectorPermission = 2;
      projectorRequiresCreative = true;
      commandsPermission = 2;
   }

   public static int getProjectorPermission() {
      return projectorPermission;
   }

   public static boolean isProjectorRequiresCreative() {
      return projectorRequiresCreative;
   }

   public static int getCommandsPermission() {
      return commandsPermission;
   }

   public static void refreshProperties() {
      if (initialized) {
         Slideshow.LOGGER.info("Refreshed Slideshow server config");

         try {
            JsonObject jsonConfig = JsonParser.parseString(String.join("", Files.readAllLines(CONFIG_PATH))).getAsJsonObject();

            try {
               projectorPermission = jsonConfig.get("projectorPermission").getAsInt();
            } catch (Exception var4) {
            }

            try {
               projectorRequiresCreative = jsonConfig.get("projectorRequiresCreative").getAsBoolean();
            } catch (Exception var3) {
            }

            try {
               commandsPermission = jsonConfig.get("commandsPermission").getAsInt();
            } catch (Exception var2) {
            }
         } catch (Exception var5) {
            writeToFile();
            refreshProperties();
         }
      }
   }

   private static void writeToFile() {
      Slideshow.LOGGER.info("Wrote Slideshow server config to file");
      JsonObject jsonConfig = new JsonObject();
      jsonConfig.addProperty("projectorPermission", projectorPermission);
      jsonConfig.addProperty("projectorRequiresCreative", projectorRequiresCreative);
      jsonConfig.addProperty("commandsPermission", commandsPermission);

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
