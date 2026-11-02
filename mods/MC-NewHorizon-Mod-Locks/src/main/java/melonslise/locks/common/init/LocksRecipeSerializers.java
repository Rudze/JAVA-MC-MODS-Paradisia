package melonslise.locks.common.init;

import melonslise.locks.Locks;
import melonslise.locks.common.recipe.KeyRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LocksRecipeSerializers
{
	public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Locks.ID);

	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<KeyRecipe>> KEY = RECIPE_SERIALIZERS.register("crafting_key", () -> new SimpleCraftingRecipeSerializer<>(KeyRecipe::new));

	private LocksRecipeSerializers() {}

	public static void register(IEventBus bus)
	{
		RECIPE_SERIALIZERS.register(bus);
	}
}
