package mdh.dndclasses.spells;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Static registry of {@link SpellClassifier}s (one per content mod that provides spells). */
public final class SpellClassifiers {

    private static final List<SpellClassifier> CLASSIFIERS = new CopyOnWriteArrayList<>();

    private SpellClassifiers() {
    }

    public static void register(SpellClassifier classifier) {
        if (classifier != null) {
            CLASSIFIERS.add(classifier);
        }
    }

    public static void clear() {
        CLASSIFIERS.clear();
    }

    public static boolean hasAny() {
        return !CLASSIFIERS.isEmpty();
    }

    public static SpellKind classify(ResourceLocation id) {
        if (id == null) {
            return SpellKind.UNKNOWN;
        }
        for (SpellClassifier classifier : CLASSIFIERS) {
            SpellKind kind = classifier.classify(id);
            if (kind != null && kind != SpellKind.UNKNOWN) {
                return kind;
            }
        }
        return SpellKind.UNKNOWN;
    }
}
