package org.teacon.slides.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class Utilities {
   private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();
   public static final Marker MARKER = MarkerManager.getMarker("Network");

   public static void sendOverLayMessage(Player player, Component message) {
      if (player instanceof ServerPlayer serverPlayer) {
         serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(message));
      }
   }

   public static int forPlayersTacking(BlockEntity entity, Consumer<ServerPlayer> consumer) {
      int i = 0;

      for (ServerPlayer player : PlayerLookup.tracking(entity)) {
         consumer.accept(player);
         i++;
      }

      return i;
   }

   public static String prettyPrint(JsonElement jsonElement) {
      return PRETTY_GSON.toJson(jsonElement);
   }
}
