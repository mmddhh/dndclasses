package mdh.dndclasses.client;

import mdh.dndclasses.weapon.DndArrowEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders the D&amp;D arrow with the vanilla 3D arrow texture as a placeholder. */
public class DndArrowRenderer extends ArrowRenderer<DndArrowEntity> {

    public DndArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(DndArrowEntity entity) {
        return TippableArrowRenderer.NORMAL_ARROW_LOCATION;
    }
}
