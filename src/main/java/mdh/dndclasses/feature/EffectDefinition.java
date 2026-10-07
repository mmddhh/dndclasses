package mdh.dndclasses.feature;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public record EffectDefinition(ResourceLocation type, JsonObject params, int index) {
}
