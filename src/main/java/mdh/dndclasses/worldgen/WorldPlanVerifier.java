package mdh.dndclasses.worldgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Structural and design checks for an M0.5 WorldPlan. Returns human-readable errors so the prototype
 * can be verified from a single command instead of counting JSON by hand.
 */
public final class WorldPlanVerifier {

    public record Result(List<String> errors, List<String> warnings) {
        public boolean ok() {
            return errors.isEmpty();
        }
    }

    private WorldPlanVerifier() {
    }

    public static Result verify(WorldPlan plan) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (plan == null) {
            errors.add("WorldPlan 为空。");
            return new Result(errors, warnings);
        }
        if (plan.nations().isEmpty()) {
            errors.add("WorldPlan 没有国家。");
        }
        if (plan.settlements().isEmpty()) {
            errors.add("WorldPlan 没有聚落。");
        }
        if (plan.events().isEmpty()) {
            errors.add("WorldPlan 没有历史事件。");
        }

        Map<Integer, WorldPlan.Nation> nations = new HashMap<>();
        Set<String> nationNames = new HashSet<>();
        for (int i = 0; i < plan.nations().size(); i++) {
            WorldPlan.Nation nation = plan.nations().get(i);
            if (nation.id() != i) {
                errors.add("国家 id 不连续：位置 " + i + " 是 " + nation.id() + "。");
            }
            if (nations.put(nation.id(), nation) != null) {
                errors.add("国家 id 重复：" + nation.id() + "。");
            }
            if (!nationNames.add(nation.name())) {
                warnings.add("国家名重复：" + nation.name() + "。");
            }
            if (nation.culture() == null || !nation.culture().contains("roof=")) {
                errors.add("国家 " + nation.id() + " 缺少文化基因组。");
            }
        }

        Map<Integer, WorldPlan.Settlement> settlements = new HashMap<>();
        Set<String> settlementNames = new HashSet<>();
        for (int i = 0; i < plan.settlements().size(); i++) {
            WorldPlan.Settlement settlement = plan.settlements().get(i);
            if (settlement.id() != i) {
                errors.add("聚落 id 不连续：位置 " + i + " 是 " + settlement.id() + "。");
            }
            if (settlements.put(settlement.id(), settlement) != null) {
                errors.add("聚落 id 重复：" + settlement.id() + "。");
            }
            WorldPlan.Nation nation = nations.get(settlement.nationId());
            if (nation == null) {
                errors.add("聚落 " + settlement.id() + " 指向不存在的国家 " + settlement.nationId() + "。");
            } else if (!settlement.culture().startsWith(nation.culture())) {
                errors.add("聚落 " + settlement.id() + " 的文化不是国家 " + nation.id() + " 文化的继承。");
            }
            if (settlement.population() <= 0) {
                errors.add("聚落 " + settlement.id() + " 人口非正数。");
            }
            if (settlement.materials().isEmpty()) {
                errors.add("聚落 " + settlement.id() + " 没有材料。");
            }
            if (new HashSet<>(settlement.materials()).size() != settlement.materials().size()) {
                errors.add("聚落 " + settlement.id() + " 材料重复：" + settlement.materials() + "。");
            }
            if (!settlementNames.add(settlement.name())) {
                warnings.add("聚落名重复：" + settlement.name() + "。");
            }
        }

        int previousYear = -1;
        Map<Long, Integer> lastYears = new HashMap<>();
        for (WorldPlan.HistoryEvent event : plan.events()) {
            if (event.year() < previousYear) {
                errors.add("事件年份倒退：" + event.year() + "。");
            }
            previousYear = event.year();
            if (event.summary() == null || event.summary().isBlank()) {
                errors.add("Y" + event.year() + " 的事件没有摘要。");
            }
            if (!switch (event.type()) {
                case "war", "alliance", "trade", "plague", "expand" -> true;
                default -> false;
            }) {
                errors.add("Y" + event.year() + " 未知事件类型 " + event.type() + "。");
            }
            if (!nations.containsKey(event.nationA())) {
                errors.add("Y" + event.year() + " nationA 越界：" + event.nationA() + "。");
            }
            if ("plague".equals(event.type()) || "expand".equals(event.type())) {
                if (event.nationB() != -1) {
                    errors.add("Y" + event.year() + " " + event.type() + " 的 nationB 应为 -1。");
                }
            } else if (!nations.containsKey(event.nationB())) {
                errors.add("Y" + event.year() + " nationB 越界：" + event.nationB() + "。");
            }
            WorldPlan.Settlement site = settlements.get(event.settlementId());
            if (site == null) {
                errors.add("Y" + event.year() + " 指向不存在的聚落 " + event.settlementId() + "。");
            } else {
                boolean allowed = site.nationId() == event.nationA()
                        || (("war".equals(event.type()) || "alliance".equals(event.type())
                        || "trade".equals(event.type())) && site.nationId() == event.nationB());
                if (!allowed) {
                    errors.add("Y" + event.year() + " 的事件地点 " + site.id()
                            + " 不属于参与国 " + event.nationA() + "/" + event.nationB() + "。");
                }
            }
            long pair = pairKey(event.nationA(), event.nationB());
            int minGap = switch (event.type()) {
                case "alliance" -> 40;
                case "trade" -> 20;
                case "war" -> 15;
                default -> 0;
            };
            if (minGap > 0) {
                long key = pair * 31 + event.type().hashCode();
                Integer last = lastYears.put(key, event.year());
                if (last != null && event.year() - last < minGap) {
                    errors.add("Y" + event.year() + " " + event.type() + " 冷却不足（上次 Y" + last + "）。");
                }
            }
        }

        return new Result(errors, warnings);
    }

    private static long pairKey(int a, int b) {
        int low = Math.min(a, b);
        int high = Math.max(a, b);
        return ((long) low << 32) | (high & 0xffffffffL);
    }
}
