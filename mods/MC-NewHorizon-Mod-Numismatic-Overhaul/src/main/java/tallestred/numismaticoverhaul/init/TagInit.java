package tallestred.numismaticoverhaul.init;

import tallestred.numismaticoverhaul.NumismaticOverhaul;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class TagInit {

    public static TagKey<Block> VERY_HEAVY_BLOCKS = blockTag("very_heavy_blocks");
    public static final TagKey<EntityType<?>> THE_BOURGEOISIE = entityTag("the_bourgeoisie");
    public static void init() {
    }
    public static TagKey<Block> blockTag(String path) {
        return BlockTags.create(ResourceLocation.fromNamespaceAndPath(NumismaticOverhaul.MODID, path));
    }

    public static TagKey<Item> itemTag(String path) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath(NumismaticOverhaul.MODID, path));
    }
    public static TagKey<EntityType<?>> entityTag(String path) {
        return TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath(NumismaticOverhaul.MODID, path));
    }
}
