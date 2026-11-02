package melonslise.locks.common.steel;

import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import melonslise.locks.common.config.LocksServerConfig;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * A recipe/advancement condition ({@code "type": "locks:steel"}) that gates a recipe on the current
 * {@link NativeSteelPolicy} decision. Unlike Forge's stock {@code forge:tag_empty}, it filters out Locks' own
 * tag entries, so it can tell whether a <em>foreign</em> mod actually provides steel even though Locks always
 * populates {@code c:ingots/steel}.
 *
 * <p>Usage — one {@code query} per condition:
 * <pre>{@code
 *   { "type": "locks:steel", "query": "native_ingot" }      // load only when the native ingot is active
 *   { "type": "locks:steel", "query": "native_nugget" }     // load only when the native nugget is active
 *   { "type": "locks:steel", "query": "steel_available" }   // load when any steel ingot exists (native or foreign)
 * }</pre>
 * Negate with the stock {@code neoforge:not} wrapper (e.g. the iron-mechanism fallback loads when steel is absent).
 */
public record NativeSteelCondition(Query query) implements ICondition
{
	public enum Query
	{
		/** Native steel ingot fallback is active. */
		NATIVE_INGOT,
		/** Native steel nugget fallback is active. */
		NATIVE_NUGGET,
		/** Native steel ore generation is active. */
		NATIVE_ORE,
		/** A steel ingot exists at all (native fallback or any foreign provider). */
		STEEL_AVAILABLE
	}

	private static final Codec<Query> QUERY_CODEC = Codec.STRING.comapFlatMap(s ->
	{
		try
		{
			return DataResult.success(Query.valueOf(s.toUpperCase(Locale.ROOT)));
		}
		catch (IllegalArgumentException e)
		{
			return DataResult.error(() -> "Unknown locks:steel condition query '" + s + "'");
		}
	}, q -> q.name().toLowerCase(Locale.ROOT));

	public static final MapCodec<NativeSteelCondition> CODEC = RecordCodecBuilder.mapCodec(builder -> builder
		.group(QUERY_CODEC.fieldOf("query").forGetter(NativeSteelCondition::query))
		.apply(builder, NativeSteelCondition::new));

	@Override
	public boolean test(IContext context)
	{
		NativeSteelState state = NativeSteelPolicy.compute(new ConditionTagView(context), currentMode());
		return switch (query)
		{
			case NATIVE_INGOT -> state.nativeIngotActive();
			case NATIVE_NUGGET -> state.nativeNuggetActive();
			case NATIVE_ORE -> state.nativeOreActive();
			case STEEL_AVAILABLE -> state.steelAvailable();
		};
	}

	/**
	 * The configured steel mode, defaulting to {@link SteelMaterialMode#AUTO} if the SERVER config is not loaded
	 * yet (recipe conditions can be evaluated in contexts where the server spec has not been populated).
	 */
	public static SteelMaterialMode currentMode()
	{
		try
		{
			if (LocksServerConfig.SPEC.isLoaded())
				return LocksServerConfig.STEEL_MATERIAL_MODE.get();
		}
		catch (RuntimeException ignored)
		{
			// Config not available in this context — fall through to AUTO.
		}
		return SteelMaterialMode.AUTO;
	}

	@Override
	public MapCodec<? extends ICondition> codec()
	{
		return CODEC;
	}

	@Override
	public String toString()
	{
		return "locks:steel(" + query.name().toLowerCase(Locale.ROOT) + ")";
	}
}
