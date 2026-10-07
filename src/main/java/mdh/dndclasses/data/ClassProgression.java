package mdh.dndclasses.data;

import java.util.Map;

public final class ClassProgression {
    private ClassProgression() {
    }

    public static final Map<String, ClassData> CLASSES = Map.ofEntries(
            Map.entry("barbarian", new ClassData(
                    new Feature("Rage", FeatureRest.SHORT, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, -1),
                    new Feature("RageDamage", FeatureRest.NONE, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4)
            )),
            Map.entry("bard", new ClassData(
                    new Feature("BardicInspiration", FeatureRest.SHORT, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1)
            )),
            Map.entry("cleric", new ClassData(
                    new Feature("ChannelDivinity", FeatureRest.SHORT, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2)
            )),
            Map.entry("druid", new ClassData(
                    new Feature("WildShape", FeatureRest.SHORT, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2)
            )),
            Map.entry("fighter", new ClassData(
                    new Feature("SecondWind", FeatureRest.SHORT, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
                    new Feature("ActionSurge", FeatureRest.SHORT, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2),
                    new Feature("Indomitable", FeatureRest.LONG, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
            )),
            Map.entry("monk", new ClassData(
                    new Feature("KiPoints", FeatureRest.SHORT, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22)
            )),
            Map.entry("paladin", new ClassData(
                    new Feature("ChannelDivinity", FeatureRest.SHORT, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2),
                    new Feature("LayOnHandsPool", FeatureRest.LONG, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100, 100)
            )),
            Map.entry("ranger", new ClassData(
                    new Feature("FavoredFoe", FeatureRest.LONG, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2)
            )),
            Map.entry("rogue", new ClassData(
                    new Feature("SneakAttackDice", FeatureRest.NONE, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 10)
            )),
            Map.entry("sorcerer", new ClassData(
                    new Feature("SorceryPoints", FeatureRest.LONG, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22)
            )),
            Map.entry("warlock", new ClassData(
                    new Feature("MysticArcanum", FeatureRest.LONG, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
            )),
            Map.entry("wizard", new ClassData(
                    new Feature("ArcaneRecovery", FeatureRest.LONG, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
            ))
    );

    public static ClassData get(String key) {
        return CLASSES.get(key);
    }

    public record ClassData(Feature... features) {
    }

    public record Feature(String name, FeatureRest restType, int... valuesPerLevel) {
        public int getValue(int level) {
            if (level < 1 || level > 20) return 0;
            return valuesPerLevel[level - 1];
        }
    }

    public enum FeatureRest {
        NONE, SHORT, LONG
    }
}
