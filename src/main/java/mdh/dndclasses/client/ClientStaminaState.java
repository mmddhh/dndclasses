package mdh.dndclasses.client;

import net.minecraft.util.Mth;

/** Last stamina values received from the server (client-side HUD state). */
public final class ClientStaminaState {

    public static volatile float stamina = 100.0F;
    public static volatile float max = 100.0F;

    private ClientStaminaState() {
    }

    public static void set(float stamina, float max) {
        ClientStaminaState.stamina = stamina;
        ClientStaminaState.max = Math.max(1.0F, max);
    }

    public static float ratio() {
        return Mth.clamp(stamina / max, 0.0F, 1.0F);
    }
}
