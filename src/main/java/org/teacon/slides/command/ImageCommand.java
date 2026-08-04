package org.teacon.slides.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.teacon.slides.config.ServerConfig;
import org.teacon.slides.network.ProjectorExportC2SPayload;

public class ImageCommand {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("image")
                  .requires(source -> source.hasPermission(ServerConfig.getCommandsPermission()) && source.getEntity() instanceof Player))
               .then(
                  Commands.literal("id")
                     .then(
                        Commands.argument("identifier", ResourceLocationArgument.id())
                           .executes(
                              context -> executeExportId((CommandSourceStack)context.getSource(), ResourceLocationArgument.getId(context, "identifier"))
                           )
                     )
               ))
            .then(
               Commands.literal("url")
                  .then(
                     Commands.argument("url_string", UnsignedMessageArgumentType.message())
                        .executes(
                           context -> executeExportUrl((CommandSourceStack)context.getSource(), UnsignedMessageArgumentType.getMessage(context, "url_string"))
                        )
                  )
            )
      );
   }

   private static int executeExportId(CommandSourceStack source, ResourceLocation id) {
      if (source.getEntity() instanceof ServerPlayer player) {
         return ProjectorExportC2SPayload.giveImageItem(player, true, id.toString()) ? 1 : 0;
      } else {
         return 0;
      }
   }

   private static int executeExportUrl(CommandSourceStack source, String url) {
      if (source.getEntity() instanceof ServerPlayer player) {
         return ProjectorExportC2SPayload.giveImageItem(player, false, url) ? 1 : 0;
      } else {
         return 0;
      }
   }
}
