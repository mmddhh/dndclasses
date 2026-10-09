package mdh.dndclasses.spells;

import net.minecraft.resources.ResourceLocation;

/**
 * Implemented by the spell-content mod (dndspell) so dndclasses can tell cantrips from leveled
 * spells without knowing the catalog. Keeps the dependency one-way (dndspell -> dndclasses).
 */
@FunctionalInterface
public interface SpellClassifier {

    SpellKind classify(ResourceLocation id);
}
