package mdh.dndclasses.client;

/**
 * Client-side state for the offhand throw: one right-click press throws once. While the use key
 * stays held after a throw, further use attempts are suppressed, so the main hand cannot throw
 * automatically once the offhand is empty.
 */
public final class OffhandThrowState {

    private static boolean consumed;

    private OffhandThrowState() {
    }

    public static boolean isConsumed() {
        return consumed;
    }

    public static void markConsumed() {
        consumed = true;
    }

    public static void reset() {
        consumed = false;
    }
}
