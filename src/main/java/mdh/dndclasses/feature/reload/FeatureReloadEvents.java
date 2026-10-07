package mdh.dndclasses.feature.reload;

import mdh.dndclasses.Dndclasses;
import mdh.dndclasses.spellcasting.SpellcastingReloadListener;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Dndclasses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FeatureReloadEvents {

    private FeatureReloadEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ClassFeatureReloadListener());
        event.addListener(new SpellcastingReloadListener());
    }
}
