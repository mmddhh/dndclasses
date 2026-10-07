package mdh.dndclasses.ability;

public final class PointBuyHelper {
    public static final int BUDGET = 27;
    public static final int MIN_SCORE = 8;

    public static int getCost(int score) {
        return switch (score) {
            case 8 -> 0; case 9 -> 1; case 10 -> 2; case 11 -> 3;
            case 12 -> 4; case 13 -> 5; case 14 -> 7; case 15 -> 9;
            default -> -1;
        };
    }

    public static int totalCost(int str, int dex, int con, int intel, int wis, int cha) {
        return getCost(str) + getCost(dex) + getCost(con) + getCost(intel) + getCost(wis) + getCost(cha);
    }

    public static boolean isValid(int str, int dex, int con, int intel, int wis, int cha) {
        return str >= MIN_SCORE && dex >= MIN_SCORE && con >= MIN_SCORE
            && intel >= MIN_SCORE && wis >= MIN_SCORE && cha >= MIN_SCORE;
    }
}
