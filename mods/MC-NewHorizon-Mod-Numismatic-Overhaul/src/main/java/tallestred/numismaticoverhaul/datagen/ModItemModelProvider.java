package tallestred.numismaticoverhaul.datagen;

/*public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, NumismaticOverhaul.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {

//        Stream.of()
//                .map(Supplier::get)
//                .forEach(this::simpleHandHeldModel);

//        Stream.of()
//                .map(Supplier::get)
//                .forEach(this::simpleGeneratedModel);

//        Stream.of()
//                .map(Supplier::get)
//                .forEach(this::simpleBlockItemModel);
    }

    protected ItemModelBuilder simpleBlockItemModel(Block block) {
        String name = getName(block);
        return withExistingParent(name, modLoc("block/" + name));
    }

    protected ItemModelBuilder simpleGeneratedModel(Item item) {
        return simpleModel(item, mcLoc("item/generated"));
    }

    protected ItemModelBuilder simpleHandHeldModel(Item item) {
        return simpleModel(item, mcLoc("item/handheld"));
    }

    protected ItemModelBuilder simpleModel(Item item, ResourceLocation parent) {
        String name = getName(item);
        return singleTexture(name, parent, "layer0", modLoc("item/" + name));
    }

    protected String getName(Item item) {
        return ForgeRegistries.ITEMS.getKey(item).getPath();
    }

    protected String getName(Block item) {
        return ForgeRegistries.BLOCKS.getKey(item).getPath();
    }

    @Override
    public ItemModelBuilder getBuilder(String path) {
        Preconditions.checkNotNull(path, "Path must not be null");
        ResourceLocation outputLoc = extendWithFolder(path.contains(":") ? new ResourceLocation(path) : new ResourceLocation(modid, path));
        this.existingFileHelper.trackGenerated(outputLoc, MODEL);
        return generatedModels.computeIfAbsent(outputLoc, loc -> new ItemModelBuilder(loc, existingFileHelper));
    }

    private ResourceLocation extendWithFolder(ResourceLocation rl) {
        if (rl.getPath().contains("/")) {
            return rl;
        }
        return new ResourceLocation(rl.getNamespace(), folder + "/" + rl.getPath());
    }
}*/
