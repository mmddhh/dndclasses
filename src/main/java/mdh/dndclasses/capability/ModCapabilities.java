package mdh.dndclasses.capability;

import mdh.dndclasses.Dndclasses;
import mdh.dndclasses.ability.Iability;
import mdh.dndclasses.level.Ilevel;
import mdh.dndclasses.race.Irace;
import mdh.dndclasses.spells.IDndSpell;
import mdh.dndclasses.spskill.Iskill;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Dndclasses.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCapabilities {
    public static final Capability<Iability> ABILITY_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<IDndSpell> DND_SPELL_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<Ilevel> LEVEL_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    // 新增技能 Capability
    public static final Capability<Iskill> SKILL_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<Irace> RACE_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(Iability.class);
        event.register(IDndSpell.class);
        event.register(Ilevel.class);
        event.register(Iskill.class);
        event.register(Irace.class);
    }
}
