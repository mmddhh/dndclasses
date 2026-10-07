package mdh.dndclasses.level;

public final class DndLeveling {
    public static final int MAX_LEVEL = 20;
    private static final int[] XP_THRESHOLDS = {
            0,
            300,
            900,
            2700,
            6500,
            14000,
            23000,
            34000,
            48000,
            64000,
            85000,
            100000,
            120000,
            140000,
            165000,
            195000,
            225000,
            265000,
            305000,
            355000
    };

    private DndLeveling() {
    }

    public static int clampExperience(int experience) {
        return Math.max(0, experience);
    }

    public static int addExperience(int currentExperience, int gainedExperience) {
        if (gainedExperience <= 0) {
            return clampExperience(currentExperience);
        }

        long total = (long) clampExperience(currentExperience) + gainedExperience;
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    public static int clampLevel(int level) {
        return Math.max(1, Math.min(MAX_LEVEL, level));
    }

    public static int getLevelForExperience(int experience) {
        int normalizedExperience = clampExperience(experience);
        int level = 1;

        for (int i = 0; i < XP_THRESHOLDS.length; i++) {
            if (normalizedExperience >= XP_THRESHOLDS[i]) {
                level = i + 1;
            } else {
                break;
            }
        }

        return level;
    }

    public static int getMinimumExperienceForLevel(int level) {
        return XP_THRESHOLDS[clampLevel(level) - 1];
    }

    public static int getProficiencyBonus(int level) {
        return 2 + (clampLevel(level) - 1) / 4;
    }
}
