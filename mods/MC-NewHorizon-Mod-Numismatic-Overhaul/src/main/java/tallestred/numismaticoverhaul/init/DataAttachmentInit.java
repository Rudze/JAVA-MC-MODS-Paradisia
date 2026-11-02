package tallestred.numismaticoverhaul.init;

import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import tallestred.numismaticoverhaul.NumismaticOverhaul;

import java.util.ArrayList;
import java.util.function.Supplier;

public class DataAttachmentInit {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NumismaticOverhaul.MODID);
    public static final Supplier<AttachmentType<Long>> VALUE = ATTACHMENT_TYPES.register(
            "value", () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).copyOnDeath().build()
    );
}
