package tallestred.numismaticoverhaul.datagen;

/*public class ModEntityLootTables extends EntityLoot {
    @Override
    protected void addTables() {
    }

    private void multiDrops(EntityType<?> type, LootEntry... entries) {
        LootPool.Builder pool = LootPool.lootPool();
        pool.setRolls(ConstantValue.exactly(1));
        for (LootEntry entry : entries) {
            pool.add(LootItem.lootTableItem(entry.getItem()).apply(SetItemCountFunction.setCount(entry.getNumberProvider())));
        }
        this.add(type, LootTable.lootTable().withPool(pool));
    }

    private void dropRange(EntityType<?> entityType, Item item, float min, float max) {
        LootTable.Builder builder = LootTable.lootTable();
        builder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(Items.BONE)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)))));
        add(entityType, builder);
    }

    private void dropSingle(EntityType<?> entityType, Item item) {
        dropSetAmount(entityType, item, 1);
    }

    private void dropSetAmount(EntityType<?> entityType, Item item, float amount) {
        LootTable.Builder builder = LootTable.lootTable();
        builder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(item)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(amount)))));
        add(entityType, builder);
    }

    @Override
    protected Iterable<EntityType<?>> getKnownEntities() {
        return List.of();
    }

    static class LootEntry {

        private final Item item;
        private final NumberProvider numberProvider;

        public LootEntry(Item item, NumberProvider numberProvider) {
            this.item = item;
            this.numberProvider = numberProvider;
        }

        public Item getItem() {
            return item;
        }

        public NumberProvider getNumberProvider() {
            return numberProvider;
        }
    }

}*/
