package tallestred.numismaticoverhaul.network;

import io.wispforest.owo.network.ClientAccess;
import tallestred.numismaticoverhaul.init.DataAttachmentInit;

public record UpdatePlayerCurrencyPacket(long value) {
    public static void handle(UpdatePlayerCurrencyPacket message, ClientAccess access) {
        access.player().setData(DataAttachmentInit.VALUE.get(), message.value());
    }
}