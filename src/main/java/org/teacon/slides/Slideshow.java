package org.teacon.slides;

import com.mojang.serialization.Codec;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarting;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopping;
//#if MC >= 26_00_00
//$$ import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
//#else
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
//#endif
//#if MC >= 12102
//$$ import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
//#endif
//#if MC >= 12111
//$$ import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
//$$ import net.minecraft.resources.Identifier;
//$$ import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
//#else
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
//#endif
//#if MC >= 26_00_00
//$$ import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
//#else
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
//#endif
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
//#if MC >= 12102
//$$ import net.minecraft.core.registries.Registries;
//#endif
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
//#if MC < 12102
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
//#endif
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.teacon.slides.command.ImageCommand;
import org.teacon.slides.command.ImageinfoCommand;
import org.teacon.slides.command.ProjectorCommand;
import org.teacon.slides.command.UnsignedMessageArgumentType;
import org.teacon.slides.config.ServerConfig;
import org.teacon.slides.item.FlipperItem;
import org.teacon.slides.item.ImageItem;
import org.teacon.slides.network.FlipperFlipBackC2SPayload;
import org.teacon.slides.network.ImageAfterUpdateC2SPayload;
import org.teacon.slides.network.ProjectorAfterUpdateC2SPayload;
import org.teacon.slides.network.ProjectorExportC2SPayload;
import org.teacon.slides.network.ProjectorImageInfoS2CPayload;
import org.teacon.slides.network.ProjectorOpenScreenPayload;
import org.teacon.slides.projector.ProjectorBlock;
import org.teacon.slides.projector.ProjectorBlockEntity;
import org.teacon.slides.projector.ProjectorScreenHandler;
import org.teacon.slides.util.RegistryServer;

public class Slideshow implements ModInitializer {
   public static final String ID = "slide_show";
   public static final Logger LOGGER = LogManager.getLogger("slide_show");
   public static final ResourceLocation PACKET_UPDATE = ResourceLocation.fromNamespaceAndPath("slide_show", "update");
   public static final ResourceLocation PACKET_EXPORT = ResourceLocation.fromNamespaceAndPath("slide_show", "export");
   public static final ResourceLocation PACKET_FLIP_BACK = ResourceLocation.fromNamespaceAndPath("slide_show", "flip_back");
   public static final ResourceLocation PACKET_TAG_UPDATE = ResourceLocation.fromNamespaceAndPath("slide_show", "tag_update");
   public static final ResourceLocation PACKET_IMAGE_UPDATE = ResourceLocation.fromNamespaceAndPath("slide_show", "image_update");
   public static MinecraftServer MC_SERVER;
//#if MC >= 12102
   //$$ public static final ResourceKey<Item> IMAGE_ITEM_KEY = keyOfItem("image");
   //$$ public static final Item IMAGE_ITEM = registerItem(new ImageItem(new Properties().stacksTo(1).setId(IMAGE_ITEM_KEY)), IMAGE_ITEM_KEY);
   //$$ public static final ResourceKey<Item> FLIPPER_ITEM_KEY = keyOfItem("flipper");
   //$$ public static final Item FLIPPER_ITEM = registerItem(
      //$$ new FlipperItem(new Properties().stacksTo(1).setId(FLIPPER_ITEM_KEY)), FLIPPER_ITEM_KEY
   //$$ );
   //$$ public static final ResourceKey<Block> PROJECTOR_BLOCK_KEY = keyOfBlock("projector");
   //$$ public static final Block PROJECTOR_BLOCK = registerBlockAndItem(
      //$$ new ProjectorBlock(
         //$$ net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
            //$$ .sound(SoundType.METAL)
            //$$ .strength(20.0F)
            //$$ .lightLevel(state -> 15)
            //$$ .noCollission()
            //$$ .setId(PROJECTOR_BLOCK_KEY)
      //$$ ),
      //$$ PROJECTOR_BLOCK_KEY
   //$$ );
   //$$ public static final BlockEntityType<ProjectorBlockEntity> PROJECTOR_BLOCK_ENTITY = (BlockEntityType<ProjectorBlockEntity>)Registry.register(
      //$$ BuiltInRegistries.BLOCK_ENTITY_TYPE,
      //$$ ResourceLocation.fromNamespaceAndPath("slide_show", "projector"),
      //$$ FabricBlockEntityTypeBuilder.create(ProjectorBlockEntity::new, new Block[]{PROJECTOR_BLOCK}).build()
   //$$ );
//#else
   public static final Item IMAGE_ITEM = registerItem("image", new ImageItem(new Properties().stacksTo(1)));
   public static final Item FLIPPER_ITEM = registerItem("flipper", new FlipperItem(new Properties().stacksTo(1)));
   public static final Block PROJECTOR_BLOCK = registerBlockAndItem(
      "projector",
      new ProjectorBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(20.0F).lightLevel(state -> 15).noCollission())
   );
   public static final BlockEntityType<ProjectorBlockEntity> PROJECTOR_BLOCK_ENTITY = (BlockEntityType<ProjectorBlockEntity>)Registry.register(
      BuiltInRegistries.BLOCK_ENTITY_TYPE,
      ResourceLocation.fromNamespaceAndPath("slide_show", "projector"),
      Builder.of(ProjectorBlockEntity::new, new Block[]{PROJECTOR_BLOCK}).build(null)
   );
//#endif
   public static final CreativeModeTab CREATIVE_TAB = (CreativeModeTab)Registry.register(
      BuiltInRegistries.CREATIVE_MODE_TAB,
      ResourceLocation.fromNamespaceAndPath("slide_show", "main"),
//#if MC >= 26_00_00
      //$$ FabricCreativeModeTab.builder()
//#else
      FabricItemGroup.builder()
//#endif
         .title(Component.translatable("itemGroup.slide_show"))
         .icon(() -> new ItemStack(PROJECTOR_BLOCK))
         .displayItems((parameters, output) -> {
            output.accept(PROJECTOR_BLOCK);
            output.accept(IMAGE_ITEM);
            output.accept(FLIPPER_ITEM);
         })
         .build()
   );
   public static final DataComponentType<List<Integer>> PROJECTOR_COMPONENT = (DataComponentType<List<Integer>>)Registry.register(
      BuiltInRegistries.DATA_COMPONENT_TYPE, ResourceLocation.fromNamespaceAndPath("slide_show", "projector"), DataComponentType.<List<Integer>>builder().persistent(Codec.INT.listOf(3, 3)).build()
   );
   public static final DataComponentType<Boolean> FROM_ID_COMPONENT = (DataComponentType<Boolean>)Registry.register(
      BuiltInRegistries.DATA_COMPONENT_TYPE, ResourceLocation.fromNamespaceAndPath("slide_show", "from_id"), DataComponentType.<Boolean>builder().persistent(Codec.BOOL).build()
   );
   public static final DataComponentType<String> LOCATION_COMPONENT = (DataComponentType<String>)Registry.register(
      BuiltInRegistries.DATA_COMPONENT_TYPE, ResourceLocation.fromNamespaceAndPath("slide_show", "location"), DataComponentType.<String>builder().persistent(Codec.STRING).build()
   );
//#if MC >= 26_00_00
   //$$ public static final ExtendedMenuType<ProjectorScreenHandler, ProjectorOpenScreenPayload> PROJECTOR_SCREEN_HANDLER = (ExtendedMenuType<ProjectorScreenHandler, ProjectorOpenScreenPayload>)Registry.register(
      //$$ BuiltInRegistries.MENU,
      //$$ Identifier.fromNamespaceAndPath("slide_show", "projector_screen_handler"),
      //$$ new ExtendedMenuType<ProjectorScreenHandler, ProjectorOpenScreenPayload>(
         //$$ (syncId, inventory, data) -> new ProjectorScreenHandler(syncId, data),
         //$$ StreamCodec.ofMember(ProjectorOpenScreenPayload::writeBuffer, ProjectorOpenScreenPayload::new)
      //$$ )
   //$$ );
//#else
   public static final ExtendedScreenHandlerType<ProjectorScreenHandler, ProjectorOpenScreenPayload> PROJECTOR_SCREEN_HANDLER = (ExtendedScreenHandlerType<ProjectorScreenHandler, ProjectorOpenScreenPayload>)Registry.register(
      BuiltInRegistries.MENU,
      ResourceLocation.fromNamespaceAndPath("slide_show", "projector_screen_handler"),
      new ExtendedScreenHandlerType<ProjectorScreenHandler, ProjectorOpenScreenPayload>(
         (syncId, inventory, data) -> new ProjectorScreenHandler(syncId, data),
         StreamCodec.ofMember(ProjectorOpenScreenPayload::writeBuffer, ProjectorOpenScreenPayload::new)
      )
   );
//#endif

   public void onInitialize() {
      RegistryServer.registerCodec(ProjectorAfterUpdateC2SPayload.ID, ProjectorAfterUpdateC2SPayload::writeBuffer, ProjectorAfterUpdateC2SPayload::new);
      RegistryServer.registerCodec(ProjectorExportC2SPayload.ID, ProjectorExportC2SPayload::writeBuffer, ProjectorExportC2SPayload::new);
      RegistryServer.registerCodec(FlipperFlipBackC2SPayload.ID, FlipperFlipBackC2SPayload::writeBuffer, FlipperFlipBackC2SPayload::new);
      RegistryServer.registerCodec(ProjectorImageInfoS2CPayload.ID, ProjectorImageInfoS2CPayload::writeBuffer, ProjectorImageInfoS2CPayload::new);
      RegistryServer.registerCodec(ImageAfterUpdateC2SPayload.ID, ImageAfterUpdateC2SPayload::writeBuffer, ImageAfterUpdateC2SPayload::new);
      RegistryServer.registerNetworkReceiver(ProjectorAfterUpdateC2SPayload.ID, ProjectorAfterUpdateC2SPayload::handle);
      RegistryServer.registerNetworkReceiver(ProjectorExportC2SPayload.ID, ProjectorExportC2SPayload::handle);
      RegistryServer.registerNetworkReceiver(FlipperFlipBackC2SPayload.ID, FlipperFlipBackC2SPayload::handle);
      RegistryServer.registerNetworkReceiver(ImageAfterUpdateC2SPayload.ID, ImageAfterUpdateC2SPayload::handle);
      ArgumentTypeRegistry.registerArgumentType(
         ResourceLocation.fromNamespaceAndPath("slide_show", "unsigned_message"),
         UnsignedMessageArgumentType.class,
         SingletonArgumentInfo.contextFree(UnsignedMessageArgumentType::message)
      );
      CommandRegistrationCallback.EVENT.register((CommandRegistrationCallback)(dispatcher, registryAccess, environment) -> {
         ImageCommand.register(dispatcher);
         ProjectorCommand.register(dispatcher);
         ImageinfoCommand.register(dispatcher);
      });
      ServerLifecycleEvents.SERVER_STARTING.register((ServerStarting)server -> {
         MC_SERVER = server;
         ServerConfig.init(server);
         ServerConfig.refreshProperties();
      });
      ServerLifecycleEvents.SERVER_STOPPING.register((ServerStopping)server -> {
         MC_SERVER = null;
         ServerConfig.uninit();
      });
//#if MC >= 26_00_00
      //$$ ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(Identifier.fromNamespaceAndPath("slide_show", "server_reload"), new ResourceManagerReloadListener() {
         //$$ public void onResourceManagerReload(ResourceManager manager) {
            //$$ ServerConfig.init(Slideshow.MC_SERVER);
            //$$ ServerConfig.refreshProperties();
         //$$ }
      //$$ });
//#elseif MC >= 12111
      //$$ ResourceLoader.get(PackType.SERVER_DATA).registerReloader(Identifier.fromNamespaceAndPath("slide_show", "server_reload"), new ResourceManagerReloadListener() {
         //$$ public void onResourceManagerReload(ResourceManager manager) {
            //$$ ServerConfig.init(Slideshow.MC_SERVER);
            //$$ ServerConfig.refreshProperties();
         //$$ }
      //$$ });
//#else
      ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
         private final ResourceLocation id = ResourceLocation.fromNamespaceAndPath("slide_show", "server_reload");

         public void onResourceManagerReload(ResourceManager manager) {
            ServerConfig.init(Slideshow.MC_SERVER);
            ServerConfig.refreshProperties();
         }

         public ResourceLocation getFabricId() {
            return this.id;
         }
      });
//#endif
   }

//#if MC >= 12102
   //$$ private static Item registerItem(Item item, ResourceKey<Item> registryKey) {
      //$$ return (Item)Registry.register(BuiltInRegistries.ITEM, registryKey, item);
   //$$ }

   //$$ private static Block registerBlockAndItem(Block block, ResourceKey<Block> registryKey) {
      //$$ Block block0 = (Block)Registry.register(BuiltInRegistries.BLOCK, registryKey, block);
      //$$ ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, registryKey.location());
      //$$ registerItem(new BlockItem(block0, new Properties().setId(key).useBlockDescriptionPrefix()), key);
      //$$ return block0;
   //$$ }

   //$$ private static ResourceKey<Item> keyOfItem(String path) {
      //$$ return ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("slide_show", path));
   //$$ }

   //$$ private static ResourceKey<Block> keyOfBlock(String path) {
      //$$ return ResourceKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("slide_show", path));
   //$$ }
//#else
   private static Item registerItem(String path, Item item) {
      return (Item)Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("slide_show", path), item);
   }

   private static Block registerBlockAndItem(String path, Block block) {
      Block block0 = (Block)Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath("slide_show", path), block);
      registerItem(path, new BlockItem(block0, new Properties()));
      return block0;
   }
//#endif
}
