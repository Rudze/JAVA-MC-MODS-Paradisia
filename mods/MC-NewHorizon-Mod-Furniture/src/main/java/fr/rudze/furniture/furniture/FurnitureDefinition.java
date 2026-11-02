package fr.rudze.furniture.furniture;

import java.util.List;

/**
 * Everything needed to turn one Nexo-style furniture config into a real block: which faces it can
 * be placed against, the shape of the cell it occupies, any extra solid/light-emitting cells it
 * drags along ("barriers"/"lights"), and its sounds.
 *
 * <p>One instance is built per converted item (see {@code nh_furniture_config/*.json}); the same
 * {@link fr.rudze.furniture.block.FurnitureBlock} class is reused for all of them. The model file
 * is used completely unmodified (drag-and-drop: the raw Blockbench/Nexo export, only its texture
 * references remapped to this mod's namespace) - the block draws it dynamically at render time
 * ({@code FurnitureBlockEntityRenderer}) using {@link #scale()} (a straight copy of the yml's own
 * "properties.scale") and the model's own baked display transform for everything else - no
 * per-item geometry math needed, ever.
 *
 * <p>Every piece faces the player on placement (chest-style, see
 * {@code FurnitureBlock#getStateForPlacement}) - Nexo's yml has a "rotatable" flag that used to make
 * this the *default* rather than the only behavior (a non-rotatable piece placed facing a fixed
 * direction unless the player was sneaking), but every piece now always faces the player regardless,
 * so that flag is no longer read at all.
 *
 * <p>{@link #color()} is the packed RGB of Nexo's yml "color" field - the tint a dyeable piece
 * (one with a tintindex-0 face in its model, see docs/adding-furniture.md) shows before it's ever
 * dyed. Undyed pieces render whatever color the model's texture actually is, tint notwithstanding.
 */
public record FurnitureDefinition(
        String id,
        String displayName,
        boolean placeFloor,
        boolean placeWall,
        boolean placeRoof,
        MainShape mainShape,
        List<Part> parts,
        int mainLight,
        boolean toggleableLight,
        String placeSound,
        String breakSound,
        double scale,
        int color) {

    /** How many of floor/wall/roof are allowed; used to decide whether the FACE blockstate property is needed. */
    public int placementCount() {
        return (placeFloor ? 1 : 0) + (placeWall ? 1 : 0) + (placeRoof ? 1 : 0);
    }

    /** The shape/collision of the origin cell (the one the master block actually occupies). */
    public record MainShape(boolean solid, double[] box) {

        public static MainShape solidCube() {
            return new MainShape(true, null);
        }

        public static MainShape interactionBox(double x0, double y0, double z0, double x1, double y1, double z1) {
            return new MainShape(false, new double[] {x0, y0, z0, x1, y1, z1});
        }

        public static MainShape empty() {
            return new MainShape(false, null);
        }
    }

    /** An extra cell, relative to the origin, dragged along when the furniture is placed. */
    public record Part(int dx, int dy, int dz, boolean solid, int light) {
    }
}
