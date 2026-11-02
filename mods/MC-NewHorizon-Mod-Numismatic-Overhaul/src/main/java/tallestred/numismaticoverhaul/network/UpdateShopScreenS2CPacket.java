package tallestred.numismaticoverhaul.network;

import io.wispforest.endec.impl.ReflectiveEndecBuilder;
import io.wispforest.owo.network.ClientAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import tallestred.numismaticoverhaul.NumismaticOverhaul;
import tallestred.numismaticoverhaul.block.ShopBlockEntity;
import tallestred.numismaticoverhaul.block.ShopOffer;
import tallestred.numismaticoverhaul.client.gui.ShopScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

public record UpdateShopScreenS2CPacket(List<ShopOffer> offers, long storedCurrency, boolean transferEnabled) {

    public UpdateShopScreenS2CPacket(ShopBlockEntity shop) {
        this(shop.getOffers(), shop.getStoredCurrency(), shop.isTransferEnabled());
    }

    public static void handle(UpdateShopScreenS2CPacket message, ClientAccess access) {
        if (!(access.runtime().screen instanceof ShopScreen screen)) return;
        screen.update(message);
    }
}
