package mdh.dndclasses;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {

    public enum StaminaHudMode {
        AUI,
        VANILLA,
        OFF
    }

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.EnumValue<StaminaHudMode> STAMINA_HUD = BUILDER
            .comment("Stamina HUD style: AUI overlay, vanilla bar, or off.")
            .defineEnum("staminaHud", StaminaHudMode.AUI);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }
}
