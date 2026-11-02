package fr.rudze.cosmetics.cosmetic;

/**
 * Everything needed to turn one Nexo/HMCCosmetics-style cosmetic config into a real, equippable
 * Accessories item: which slot it goes in, and what color it shows before it's ever dyed.
 *
 * <p>One instance is built per converted item (see {@code nh_cosmetics_config/*.json}); the same
 * {@link fr.rudze.cosmetics.item.CosmeticItem} class is reused for all of them. The model file is
 * used completely unmodified (drag-and-drop: the raw Blockbench/Nexo export, only its texture
 * references remapped to this mod's namespace).
 *
 * <p>{@link #slot()} matches one of Accessories' built-in slot names ("hat", "back", "hand") - this
 * mod adds no custom slots. It decides both which Accessories slot the item tag is added to and how
 * {@code CosmeticAccessoryRenderer} positions it: "hat" anchors on the head bone using the model's
 * own baked "head" display transform (matching vanilla's {@code CustomHeadLayer}); "hand" anchors on
 * the arm bone using the model's own baked "thirdperson_*" transform. "back" is different - unlike
 * the other slots, these models aren't rendered through their own baked transform at all;
 * {@link #backScale()}/{@link #backOffsetX()}/{@link #backOffsetY()}/{@link #backOffsetZ()} control
 * the raw placement directly (same technique as the standalone Backpack module's own renderer),
 * since geometry this different (a cloak vs. a pair of wings vs. a float) needs its own per-item
 * tuning rather than one shared transform. Defaults match the Backpack module's own tuned starting
 * values.
 *
 * <p>{@link #balloon()} is an orthogonal flag, only meaningful when {@link #slot()} is "hand": a
 * balloon shares the same equip slot as a held cosmetic (staff, trident, etc. - same pool of 2 "hand"
 * accessory slots) but renders completely differently - floating above the player on a leash-style
 * rope instead of held in a fist. See {@code CosmeticAccessoryRenderer#renderBalloon}.
 *
 * <p>{@link #color()} is the packed RGB of Nexo's yml "color" field - the tint a dyeable piece
 * (one with a tintindex-0 face in its model) shows before it's ever dyed.
 */
public record CosmeticDefinition(
        String id,
        String displayName,
        String slot,
        boolean balloon,
        int color,
        double backScale,
        double backOffsetX,
        double backOffsetY,
        double backOffsetZ) {
}
