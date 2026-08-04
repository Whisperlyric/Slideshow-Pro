package org.teacon.slides.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.teacon.slides.config.ServerConfig;
import org.teacon.slides.projector.ProjectorBlockEntity;
import org.teacon.slides.projector.SourceType;

public class ImageinfoCommand {
   private static final Component IMAGE_INFO_CHANGED = Component.translatable("chat.slide_show.image_info_changed");

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("imageinfo")
               .requires(source -> source.hasPermission(ServerConfig.getCommandsPermission())))
            .then(
               ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("pos", BlockPosArgument.blockPos())
                           .then(
                              ((LiteralArgumentBuilder)Commands.literal("id")
                                    .executes(
                                       context -> execute(
                                             (CommandSourceStack)context.getSource(),
                                             BlockPosArgument.getBlockPos(context, "pos"),
                                             SourceType.ResourceID,
                                             null
                                          )
                                    ))
                                 .then(
                                    Commands.argument("location", StringArgumentType.string())
                                       .executes(
                                          context -> execute(
                                                (CommandSourceStack)context.getSource(),
                                                BlockPosArgument.getBlockPos(context, "pos"),
                                                SourceType.ResourceID,
                                                StringArgumentType.getString(context, "location")
                                             )
                                       )
                                 )
                           ))
                        .then(
                           ((LiteralArgumentBuilder)Commands.literal("url")
                                 .executes(
                                    context -> execute(
                                          (CommandSourceStack)context.getSource(), BlockPosArgument.getBlockPos(context, "pos"), SourceType.URL, null
                                       )
                                 ))
                              .then(
                                 Commands.argument("location", StringArgumentType.string())
                                    .executes(
                                       context -> execute(
                                             (CommandSourceStack)context.getSource(),
                                             BlockPosArgument.getBlockPos(context, "pos"),
                                             SourceType.URL,
                                             StringArgumentType.getString(context, "location")
                                          )
                                    )
                              )
                        ))
                     .then(
                        ((LiteralArgumentBuilder)Commands.literal("container")
                              .executes(
                                 context -> execute(
                                       (CommandSourceStack)context.getSource(),
                                       BlockPosArgument.getBlockPos(context, "pos"),
                                       SourceType.ContainerBlock,
                                       null
                                    )
                              ))
                           .then(
                              Commands.argument("location", StringArgumentType.string())
                                 .executes(
                                    context -> execute(
                                          (CommandSourceStack)context.getSource(),
                                          BlockPosArgument.getBlockPos(context, "pos"),
                                          SourceType.ContainerBlock,
                                          StringArgumentType.getString(context, "location")
                                       )
                                 )
                           )
                     ))
                  .then(
                     Commands.argument("location", StringArgumentType.string())
                        .executes(
                           context -> execute(
                                 (CommandSourceStack)context.getSource(),
                                 BlockPosArgument.getBlockPos(context, "pos"),
                                 null,
                                 StringArgumentType.getString(context, "location")
                              )
                        )
                  )
            )
      );
   }

   private static int execute(CommandSourceStack source, BlockPos blockPos, @Nullable SourceType sourceType, @Nullable String location) {
      ServerLevel world = source.getLevel();
      if (!world.isLoaded(blockPos)) {
         return 0;
      } else if (world.getBlockEntity(blockPos) instanceof ProjectorBlockEntity projectorBlockEntity) {
         if (sourceType == null && location == null) {
            return 0;
         } else {
            if (sourceType != null) {
               projectorBlockEntity.mSourceType = sourceType;
            }

            if (location != null) {
               projectorBlockEntity.mLocation = location;
            }

            projectorBlockEntity.setChanged();
            BlockState state = projectorBlockEntity.getBlockState();
            if (!world.setBlock(blockPos, state, 3)) {
               world.sendBlockUpdated(blockPos, state, state, 2);
            }

            source.sendSystemMessage(IMAGE_INFO_CHANGED);
            return 1;
         }
      } else {
         source.sendFailure(ProjectorCommand.NO_PROJECTOR);
         return 0;
      }
   }
}
