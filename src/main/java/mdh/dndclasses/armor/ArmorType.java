package mdh.dndclasses.armor;

public enum ArmorType {
    // id, category, base AC, strength requirement, stealth disadvantage
    PADDED("padded", ArmorCategory.LIGHT, 11, 0, true),
    LEATHER("leather", ArmorCategory.LIGHT, 11, 0, false),
    STUDDED_LEATHER("studded_leather", ArmorCategory.LIGHT, 12, 0, false),
    HIDE("hide", ArmorCategory.MEDIUM, 12, 0, false),
    CHAIN_SHIRT("chain_shirt", ArmorCategory.MEDIUM, 13, 0, false),
    SCALE_MAIL("scale_mail", ArmorCategory.MEDIUM, 14, 0, true),
    BREASTPLATE("breastplate", ArmorCategory.MEDIUM, 14, 0, false),
    HALF_PLATE("half_plate", ArmorCategory.MEDIUM, 15, 0, true),
    RING_MAIL("ring_mail", ArmorCategory.HEAVY, 14, 0, true),
    CHAIN_MAIL("chain_mail", ArmorCategory.HEAVY, 16, 13, true),
    SPLINT("splint", ArmorCategory.HEAVY, 17, 15, true),
    PLATE("plate", ArmorCategory.HEAVY, 18, 15, true);

    private final String id;
    private final ArmorCategory category;
    private final int baseAc;
    private final int strengthRequirement;
    private final boolean stealthDisadvantage;

    ArmorType(String id, ArmorCategory category, int baseAc, int strengthRequirement, boolean stealthDisadvantage) {
        this.id = id;
        this.category = category;
        this.baseAc = baseAc;
        this.strengthRequirement = strengthRequirement;
        this.stealthDisadvantage = stealthDisadvantage;
    }

    public String id() {
        return id;
    }

    public ArmorCategory category() {
        return category;
    }

    public int baseAc() {
        return baseAc;
    }

    public int strengthRequirement() {
        return strengthRequirement;
    }

    public boolean stealthDisadvantage() {
        return stealthDisadvantage;
    }
}
