package mdh.dndclasses.compat;

/**
 * Small platform seam for client-only side effects. Server/content packages call these hooks
 * instead of importing {@code net.minecraft.client} classes directly; the client registers the real
 * implementation during client setup.
 */
public final class ClientHooks {

    private static Runnable clearReloadAnimation = () -> {
    };

    private ClientHooks() {
    }

    public static void setClearReloadAnimation(Runnable implementation) {
        clearReloadAnimation = implementation == null ? () -> {
        } : implementation;
    }

    public static void clearReloadAnimation() {
        clearReloadAnimation.run();
    }
}
