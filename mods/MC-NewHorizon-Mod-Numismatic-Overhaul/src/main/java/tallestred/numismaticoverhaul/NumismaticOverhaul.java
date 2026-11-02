package tallestred.numismaticoverhaul;


import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.MapCodec;
import io.wispforest.endec.impl.ReflectiveEndecBuilder;
import io.wispforest.owo.network.OwoNetChannel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import tallestred.numismaticoverhaul.block.ShopOffer;
import tallestred.numismaticoverhaul.config.NOClientConfig;
import tallestred.numismaticoverhaul.config.NOConfig;
import tallestred.numismaticoverhaul.datagen.ModLootTableProvider;
import tallestred.numismaticoverhaul.economy.CropConfig;
import tallestred.numismaticoverhaul.economy.CropMarket;
import tallestred.numismaticoverhaul.economy.Crops;
import tallestred.numismaticoverhaul.economy.EconomyWebServer;
import tallestred.numismaticoverhaul.init.*;
import tallestred.numismaticoverhaul.loot_stuff.AddItemModifier;
import tallestred.numismaticoverhaul.loot_stuff.MoneyBagLootModifier;
import tallestred.numismaticoverhaul.network.RequestPurseActionC2SPacket;
import tallestred.numismaticoverhaul.network.ShopScreenHandlerRequestC2SPacket;
import tallestred.numismaticoverhaul.network.UpdatePlayerCurrencyPacket;
import tallestred.numismaticoverhaul.network.UpdateShopScreenS2CPacket;
import tallestred.numismaticoverhaul.villagers.json.VillagerTradesHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mod(NumismaticOverhaul.MODID)
public class NumismaticOverhaul {
    public static final String MODID = "numismaticoverhaul";
    public static final Logger LOGGER = LogManager.getLogger();
    //public static final LootPoolEntryType MONEY_BAG_ENTRY = new LootPoolEntryType(new MoneyBagLootEntry.Serializer());
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<? extends IGlobalLootModifier>> ADD_ITEM =
            LOOT_MODIFIER_SERIALIZERS.register("add_item", AddItemModifier.CODEC);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<? extends IGlobalLootModifier>> MONEY_BAG =
            LOOT_MODIFIER_SERIALIZERS.register("money_bag", MoneyBagLootModifier.CODEC);
    public static final OwoNetChannel MY_CHANNEL = OwoNetChannel.create(ResourceLocation.fromNamespaceAndPath(MODID, "main"));

    public NumismaticOverhaul(IEventBus bus, Dist dist, ModContainer container) {
        container.registerConfig(ModConfig.Type.STARTUP, NOConfig.CONFIG_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, NOClientConfig.CLIENT_SPEC);
        LOOT_MODIFIER_SERIALIZERS.register(bus);
        ItemInit.ITEMS.register(bus);
        EntityInit.ENTITIES.register(bus);
        BlockInit.BLOCKS.register(bus);
        BlockInit.BLOCK_ENTITIES.register(bus);
        CreativeTabInit.TABS.register(bus);
        MenuInit.MENU_TYPES.register(bus);
        DataAttachmentInit.ATTACHMENT_TYPES.register(bus);
        ItemComponentInit.DATA_COMPONENTS.register(bus);
        VillagerTradesHandler.registerDefaultAdapters();
        bus.addListener(this::onCommonSetup);
        bus.addListener(this::onGatherData);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal(MODID)
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("market")
                                .then(Commands.literal("reset")
                                        .then(Commands.argument("crop", StringArgumentType.word())
                                                .suggests(NumismaticOverhaul::suggestCropOrAll)
                                                .executes(this::resetMarket))))
        );
    }

    private static CompletableFuture<Suggestions> suggestCropOrAll(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<String> options = new ArrayList<>();
        options.add("all");
        Crops.ALL.forEach(crop -> options.add(crop.commodityId()));
        return SharedSuggestionProvider.suggest(options, builder);
    }

    /** {@code /numismaticoverhaul market reset <crop|all>} - one or every crop back to {@link CropMarket#BASE_STOCK}. */
    private int resetMarket(CommandContext<CommandSourceStack> context) {
        String cropId = StringArgumentType.getString(context, "crop");
        var overworld = context.getSource().getServer().overworld();

        List<CropConfig> targets = cropId.equals("all")
                ? Crops.ALL
                : Crops.ALL.stream().filter(crop -> crop.commodityId().equals(cropId)).toList();

        if (targets.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Unknown crop: " + cropId));
            return 0;
        }

        for (CropConfig crop : targets) {
            CropMarket market = CropMarket.get(overworld, crop);
            int previousStock = market.getStock();
            market.resetStock(overworld.getGameTime());
            context.getSource().sendSuccess(() -> PREFIX.copy().append(Component.literal(
                    crop.displayName() + " stock reset (was " + previousStock + ", now " + CropMarket.BASE_STOCK + ")."
            )), true);
        }
        return targets.size();
    }

    // Starts/stops with the integrated or dedicated server (not FMLCommonSetupEvent) since the
    // dashboard has nothing to serve - and no world to read stock/history from - before a server
    // actually exists.
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        EconomyWebServer.start();
        // Publish an initial snapshot right away instead of waiting for the first player interaction
        // with a WheatMerchantEntity (which is what normally triggers CropMarket#get) - otherwise the
        // dashboard shows this merchant as locked/no-shop until someone happens to open its trade screen.
        for (CropConfig crop : Crops.ALL) {
            CropMarket.get(event.getServer().overworld(), crop);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        EconomyWebServer.stop();
    }

    @SubscribeEvent
    public void onCommonSetup(FMLCommonSetupEvent event) {
        ReflectiveEndecBuilder.SHARED_INSTANCE.register(ShopOffer.ENDEC, ShopOffer.class);
        MY_CHANNEL.registerClientbound(UpdateShopScreenS2CPacket.class, UpdateShopScreenS2CPacket::handle);
        MY_CHANNEL.registerClientbound(UpdatePlayerCurrencyPacket.class, UpdatePlayerCurrencyPacket::handle);
        MY_CHANNEL.registerServerbound(RequestPurseActionC2SPacket.class, RequestPurseActionC2SPacket::handle);
        MY_CHANNEL.registerServerbound(ShopScreenHandlerRequestC2SPacket.class, ShopScreenHandlerRequestC2SPacket::handle);
    }

    @SubscribeEvent
    public void onGatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        PackOutput packOutput = event.getGenerator().getPackOutput();
        //   generator.addProvider(event.includeServer(), new ModRecipeProvider(generator));
        generator.addProvider(event.includeServer(), new ModLootTableProvider(packOutput, event.getLookupProvider()));
        //   generator.addProvider(event.includeServer(), new ModSoundProvider(generator, MODID, existingFileHelper));
        //    generator.addProvider(event.includeClient(), new ModItemModelProvider(generator, existingFileHelper));
        //  generator.addProvider(event.includeClient(), new ModBlockStateProvider(generator, existingFileHelper));
        //   generator.addProvider(event.includeClient(), new ModLangProvider(generator, MODID, "en_us"));
    }

    public static final Component PREFIX = Component.empty().withStyle(ChatFormatting.GRAY)
            .append(withColor("o", 0x3955e5))
            .append(withColor("ω", 0x13a6f0))
            .append(withColor("o", 0x3955e5))
            .append(Component.literal(" > ").withStyle(ChatFormatting.GRAY));

    public static MutableComponent withColor(String text, int color) {
        return Component.literal(text).setStyle(Style.EMPTY.withColor(color));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}

