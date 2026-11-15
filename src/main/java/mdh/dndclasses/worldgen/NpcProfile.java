package mdh.dndclasses.worldgen;

/**
 * Data-only NPC profile. It is the hand-off between the planning layer and the entity layer; the
 * entity adapter may materialize it as a villager now and as a custom NPC entity later.
 */
public record NpcProfile(String name, String race, String role, int settlementId, int nationId,
                         String culture, String profession) {
}
