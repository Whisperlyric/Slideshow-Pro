package org.teacon.slides.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.teacon.slides.config.ServerConfig;
import org.teacon.slides.projector.ProjectorBlockEntity;

public class ProjectorCommand {
   protected static final Component NO_PROJECTOR = Component.translatable("chat.slide_show.no_projector");
   private static final Component CANNOT_FLIP = Component.translatable("chat.slide_show.cannot_flip");
   private static final Component INITIALIZED = Component.translatable("chat.slide_show.initialized");
   private static final Component FLIPPED = Component.translatable("chat.slide_show.flipped");

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("projector")
                     .requires(source -> source.hasPermission(ServerConfig.getCommandsPermission())))
                  .then(
                     Commands.literal("next")
                        .then(
                           Commands.argument("pos", BlockPosArgument.blockPos())
                              .executes(
                                 context -> executeFlip(
                                       (CommandSourceStack)context.getSource(), BlockPosArgument.getBlockPos(context, "pos"), false, false
                                    )
                              )
                        )
                  ))
               .then(
                  Commands.literal("prev")
                     .then(
                        Commands.argument("pos", BlockPosArgument.blockPos())
                           .executes(
                              context -> executeFlip((CommandSourceStack)context.getSource(), BlockPosArgument.getBlockPos(context, "pos"), true, false)
                           )
                     )
               ))
            .then(
               Commands.literal("first")
                  .then(
                     Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(
                           context -> executeFlip((CommandSourceStack)context.getSource(), BlockPosArgument.getBlockPos(context, "pos"), false, true)
                        )
                  )
            )
      );
   }

   public static int executeFlip(CommandSourceStack source, BlockPos pos, boolean back, boolean init) {
      if (source.getLevel().getBlockEntity(pos) instanceof ProjectorBlockEntity entity1) {
         if (!entity1.canFlip()) {
            source.sendFailure(CANNOT_FLIP);
            return 0;
         } else if (init) {
            entity1.needInitContainer = true;
            source.sendSystemMessage(INITIALIZED);
            return 1;
         } else {
            entity1.needHandleReadImage = true;
            if (back) {
               entity1.flipBack = true;
            }

            source.sendSystemMessage(FLIPPED);
            return 1;
         }
      } else {
         source.sendFailure(NO_PROJECTOR);
         return 0;
      }
   }
}
