
/* ------------------------------------------------------------------ page logic */

var BUDGET = 27;
var MIN_SCORE = 8;
var MAX_SCORE = 15;
var POINT_COST = { 8: 0, 9: 1, 10: 2, 11: 3, 12: 4, 13: 5, 14: 7, 15: 9 };


var ABILITIES = [
  { key: "str" }, { key: "dex" }, { key: "con" },
  { key: "int" }, { key: "wis" }, { key: "cha" }
];

var TABS = [
  "screen.dndclasses.creation.tab.overview",
  "screen.dndclasses.creation.tab.levels",
  "screen.dndclasses.creation.tab.subclass",
  "screen.dndclasses.creation.tab.equipment"
];

/* 瀛愯亴鍏冩暟鎹細璇ヨ亴涓?瀛愯亴閫夋嫨绛夌骇 + 瀛愯亴 ID 鏈夊簭琛ㄣ€? * 鏄剧ず鍚嶇敱 id 鐨?path 鐢熸垚閿?subclass.dndclasses.<path>.name銆? * 鍙湁閫夋嫨绛夌骇 <= 1 鐨勮亴涓氬彲鍦ㄥ垱寤虹晫闈㈤€夋嫨锛屽叾浣欎粎浣滈瑙堛€?*/
var SUBCLASS_META = {
  barbarian: { level: 3, ids: ["dndclasses:berserker", "dndclasses:totem_warrior"] },
  bard: { level: 3, ids: ["dndclasses:lore", "dndclasses:valor"] },
  cleric: { level: 1, ids: [
    "dndclasses:knowledge", "dndclasses:life", "dndclasses:light", "dndclasses:nature",
    "dndclasses:tempest", "dndclasses:trickery", "dndclasses:war"] },
  druid: { level: 2, ids: ["dndclasses:land", "dndclasses:moon"] },
  fighter: { level: 3, ids: ["dndclasses:champion", "dndclasses:battle_master", "dndclasses:eldritch_knight"] },
  monk: { level: 3, ids: ["dndclasses:open_hand", "dndclasses:shadow", "dndclasses:four_elements"] },
  paladin: { level: 3, ids: ["dndclasses:devotion", "dndclasses:ancients", "dndclasses:vengeance"] },
  ranger: { level: 3, ids: ["dndclasses:hunter", "dndclasses:beast_master"] },
  rogue: { level: 3, ids: ["dndclasses:thief", "dndclasses:assassin", "dndclasses:arcane_trickster"] },
  sorcerer: { level: 1, ids: ["dndclasses:draconic_bloodline", "dndclasses:wild_magic"] },
  warlock: { level: 1, ids: ["dndclasses:archfey", "dndclasses:fiend", "dndclasses:great_old_one"] },
  wizard: { level: 2, ids: [
    "dndclasses:abjuration", "dndclasses:conjuration", "dndclasses:divination", "dndclasses:enchantment",
    "dndclasses:evocation", "dndclasses:illusion", "dndclasses:necromancy", "dndclasses:transmutation"] }
};

function subclassMeta(c) {
  return SUBCLASS_META[c.key] || { level: 3, ids: [] };
}

function subclassNameKey(c, index) {
  var meta = subclassMeta(c);
  if (!meta.ids || index < 0 || index >= meta.ids.length) return "";
  var id = String(meta.ids[index]);
  var path = id.indexOf(":") >= 0 ? id.split(":")[1] : id;
  return "subclass.dndclasses." + path + ".name";
}

function itemName(id) {
  var s = String(id);
  var ns = "minecraft", path = s;
  if (s.indexOf(":") >= 0) { ns = s.split(":")[0]; path = s.split(":")[1]; }
  return t("item." + ns + "." + path);
}

function eq(items) { return { items: items }; }

function armorSet(prefix) {
  return [prefix + "_helmet", prefix + "_chestplate", prefix + "_leggings", prefix + "_boots"];
}


var CLASSES = [
  {
    key: "barbarian", hitDie: "D12",
    primaryKey: "class.dndclasses.barbarian.primary", primaryAbilities: ["str"],
    saves: ["str", "con"],
    armor: ["light", "medium", "shield"], weapons: ["martial", "simple"],
    equipment: [
      eq(["dndclasses:iron_greataxe", "dndclasses:iron_handaxe", "dndclasses:iron_handaxe"]),
      eq(["dndclasses:iron_longsword"])
    ]
  },
  {
    key: "bard", hitDie: "D8",
    primaryKey: "class.dndclasses.bard.primary", primaryAbilities: ["cha"],
    saves: ["dex", "cha"],
    armor: ["light"], weapons: ["hand_crossbow", "longsword", "rapier", "shortsword", "simple"],
    equipment: [
      eq(armorSet("dndclasses:iron_leather").concat(["dndclasses:iron_rapier", "dndclasses:iron_dagger"])),
      eq(armorSet("dndclasses:iron_leather").concat(["dndclasses:iron_longsword", "dndclasses:iron_dagger"]))
    ]
  },
  {
    key: "cleric", hitDie: "D8",
    primaryKey: "class.dndclasses.cleric.primary", primaryAbilities: ["wis"],
    saves: ["wis", "cha"],
    armor: ["light", "medium", "shield"], weapons: ["simple"],
    equipment: [
      eq(armorSet("dndclasses:iron_scale_mail").concat(["dndclasses:iron_mace", "minecraft:shield"])),
      eq(armorSet("dndclasses:iron_scale_mail").concat(["dndclasses:iron_warhammer", "minecraft:shield"]))
    ]
  },
  {
    key: "druid", hitDie: "D8",
    primaryKey: "class.dndclasses.druid.primary", primaryAbilities: ["wis"],
    saves: ["int", "wis"],
    armor: ["light", "medium", "shield_nonmetal"], weapons: ["simple", "scimitar", "quarterstaff", "sling"],
    equipment: [
      eq(armorSet("dndclasses:iron_leather").concat(["dndclasses:iron_quarterstaff"])),
      eq(armorSet("dndclasses:iron_leather").concat(["dndclasses:iron_scimitar", "minecraft:shield"]))
    ]
  },
  {
    key: "fighter", hitDie: "D10",
    primaryKey: "class.dndclasses.fighter.primary", primaryAbilities: ["str", "dex"],
    saves: ["str", "con"],
    armor: ["all", "shield"], weapons: ["martial", "simple"],
    equipment: [
      eq(armorSet("dndclasses:iron_chain_mail").concat(["minecraft:shield", "dndclasses:iron_longsword"])),
      eq(armorSet("dndclasses:iron_chain_mail").concat(["dndclasses:iron_greatsword"]))
    ]
  },
  {
    key: "monk", hitDie: "D8",
    primaryKey: "class.dndclasses.monk.primary", primaryAbilities: ["dex", "wis"],
    saves: ["str", "dex"],
    armor: ["none"], weapons: ["simple", "shortsword"],
    equipment: [
      eq(["dndclasses:iron_shortsword", "dndclasses:iron_dagger"]),
      eq(["dndclasses:iron_quarterstaff"])
    ]
  },
  {
    key: "paladin", hitDie: "D10",
    primaryKey: "class.dndclasses.paladin.primary", primaryAbilities: ["str", "cha"],
    saves: ["wis", "cha"],
    armor: ["all", "shield"], weapons: ["martial", "simple"],
    equipment: [
      eq(armorSet("dndclasses:iron_chain_mail").concat(["minecraft:shield", "dndclasses:iron_longsword"])),
      eq(armorSet("dndclasses:iron_plate").concat(["dndclasses:iron_greatsword"]))
    ]
  },
  {
    key: "ranger", hitDie: "D10",
    primaryKey: "class.dndclasses.ranger.primary", primaryAbilities: ["dex", "wis"],
    saves: ["str", "dex"],
    armor: ["light", "medium", "shield"], weapons: ["martial", "simple"],
    equipment: [
      eq(armorSet("dndclasses:iron_leather").concat(["dndclasses:iron_shortsword", "dndclasses:iron_longbow", "dndclasses:iron_arrow"])),
      eq(armorSet("dndclasses:iron_leather").concat(["dndclasses:iron_shortsword", "dndclasses:iron_shortsword"]))
    ]
  },
  {
    key: "rogue", hitDie: "D8",
    primaryKey: "class.dndclasses.rogue.primary", primaryAbilities: ["dex"],
    saves: ["dex", "int"],
    armor: ["light"], weapons: ["hand_crossbow", "longsword", "rapier", "shortsword", "simple"],
    equipment: [
      eq(armorSet("dndclasses:iron_leather").concat(["dndclasses:iron_rapier", "dndclasses:iron_shortbow", "dndclasses:iron_arrow"])),
      eq(armorSet("dndclasses:iron_leather").concat(["dndclasses:iron_shortsword", "dndclasses:iron_shortsword"]))
    ]
  },
  {
    key: "sorcerer", hitDie: "D6",
    primaryKey: "class.dndclasses.sorcerer.primary", primaryAbilities: ["cha"],
    saves: ["con", "cha"],
    armor: ["none"], weapons: ["dagger", "sling", "quarterstaff", "light_crossbow"],
    equipment: [
      eq(["dndclasses:iron_light_crossbow", "dndclasses:iron_dagger", "dndclasses:iron_bolt"]),
      eq(["dndclasses:iron_quarterstaff", "dndclasses:iron_dagger"])
    ]
  },
  {
    key: "warlock", hitDie: "D8",
    primaryKey: "class.dndclasses.warlock.primary", primaryAbilities: ["cha"],
    saves: ["wis", "cha"],
    armor: ["light"], weapons: ["simple"],
    equipment: [
      eq(["dndclasses:iron_light_crossbow", "dndclasses:iron_dagger", "dndclasses:iron_bolt"]),
      eq(["dndclasses:iron_quarterstaff", "dndclasses:iron_dagger"])
    ]
  },
  {
    key: "wizard", hitDie: "D6",
    primaryKey: "class.dndclasses.wizard.primary", primaryAbilities: ["int"],
    saves: ["int", "wis"],
    armor: ["none"], weapons: ["dagger", "sling", "quarterstaff", "light_crossbow"],
    equipment: [
      eq(["dndclasses:iron_quarterstaff", "dndclasses:iron_dagger"]),
      eq(["dndclasses:iron_light_crossbow", "dndclasses:iron_dagger", "dndclasses:iron_bolt"])
    ]
  }
];

/* 璇█涓珛鐨勮亴涓氬浘鏍囷細鍐呰仈 SVG锛岄鑹茬户鎵?.seal 鐨?color锛堥噾鑹诧級銆? * 绔?妯瑪鐢荤敤 rect 鑰岄潪 line锛欰UI 鐨?Svg 娓叉煋浼氳烦杩囬浂闈㈢Н鐨勯€€鍖栫瑪鐢汇€?*/
function svg(inner) {
  return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" '
    + 'stroke-linecap="round" stroke-linejoin="round">' + inner + '</svg>';
}

function bar(x, y, w, h) {
  return '<rect x="' + x + '" y="' + y + '" width="' + w + '" height="' + h
    + '" fill="currentColor" stroke="none"/>';
}

var SEALS = {
  barbarian: svg(bar(12, 4, 2, 17) + '<polygon points="13,4 21,7 20,13 13,11"/>'),
  bard: svg('<circle cx="7" cy="18" r="3"/><circle cx="17" cy="16" r="3"/>'
    + bar(9, 6, 2, 12) + bar(19, 5, 2, 11) + '<line x1="10" y1="6" x2="20" y2="5"/>'),
  cleric: svg(bar(11, 3, 2, 18) + bar(6, 8, 12, 2)),
  druid: svg('<polygon points="12,3 19,10 12,21 5,10"/>' + bar(11, 8, 2, 13)),
  fighter: svg(bar(11, 5, 2, 16) + bar(7, 9, 10, 2) + '<polygon points="12,2 14,6 10,6"/>'),
  monk: svg('<rect x="9" y="10" width="6" height="9"/>'
    + bar(8, 6, 2, 4) + bar(11, 5, 2, 5) + bar(14, 6, 2, 4) + bar(6, 12, 3, 2)),
  paladin: svg('<polygon points="12,3 20,6 20,12 12,21 4,12 4,6"/>'
    + bar(11, 7, 2, 8) + bar(8, 9, 8, 2)),
  ranger: svg('<line x1="5" y1="19" x2="19" y2="5"/><polygon points="19,5 12,5 19,12"/>'
    + '<line x1="11" y1="13" x2="7" y2="17"/>'),
  rogue: svg('<polygon points="12,3 15,10 9,10"/>' + bar(11, 10, 2, 8) + bar(9, 17, 6, 2)),
  sorcerer: svg('<polygon points="12,2 14,10 22,12 14,14 12,22 10,14 2,12 10,10"/>'),
  warlock: svg('<ellipse cx="12" cy="12" rx="9" ry="5"/><circle cx="12" cy="12" r="2.5"/>'),
  wizard: svg('<polygon points="12,3 18,16 6,16"/>' + bar(4, 15, 16, 2) + '<circle cx="12" cy="11" r="1"/>')
};

var state = {
  selected: null,
  equipment: 0,
  tab: 0,
  subclass: 0,
  subclassChosen: null,
  abilities: { str: 8, dex: 8, con: 8, int: 8, wis: 8, cha: 8 },
  raceBonuses: { str: 0, dex: 0, con: 0, int: 0, wis: 0, cha: 0 }
};

// AUI's `window` is a Java object and cannot hold JS properties, so shared state lives in plain
// script globals instead.
var DND_RACE = null;

function racePoints() {
  return DND_RACE ? (DND_RACE.points || 0) : 0;
}

function raceSpent() {
  var total = 0;
  for (var key in state.raceBonuses) {
    total += state.raceBonuses[key] || 0;
  }
  return total;
}

function byId(id) { return document.getElementById(id); }

/* ---- i18n helpers --------------------------------------------------------
 * Text is rendered through AUI's <translation> element, which resolves the key
 * against the client language. There is no parameter interpolation, so any
 * "label + value" text is built as a translated label plus a plain value. */
function t(key) { return "<translation>" + key + "</translation>"; }

function mk(tag, cls, key) {
  var e = document.createElement(tag);
  if (cls) e.setAttribute("class", cls);
  if (key !== undefined && key !== null && key !== "") e.innerHTML = t(key);
  return e;
}

function abilName(key) { return t("ability.dndclasses." + key); }


function make(tag, cls, text) {
  var e = document.createElement(tag);
  if (cls) e.setAttribute("class", cls);
  if (text !== undefined && text !== null) e.textContent = String(text);
  return e;
}

function clearNode(node) {
  var kids = node.getChildren();
  for (var i = kids.size() - 1; i >= 0; i--) {
    node.removeChild(kids.get(i));
  }
}

function findClass(key) {
  for (var i = 0; i < CLASSES.length; i++) {
    if (CLASSES[i].key === key) return CLASSES[i];
  }
  return null;
}

function itemTexture(id) {
  var parts = String(id).split(":");
  var ns = parts.length > 1 ? parts[0] : "minecraft";
  var path = parts.length > 1 ? parts[1] : parts[0];
  return ns + ":textures/item/" + path + ".png";
}

function profBonus(level) {
  return 2 + Math.floor((level - 1) / 4);
}

function scoreCost(score) {
  return POINT_COST[score] === undefined ? -1 : POINT_COST[score];
}

function totalCost() {
  var sum = 0;
  for (var i = 0; i < ABILITIES.length; i++) {
    sum += scoreCost(state.abilities[ABILITIES[i].key]);
  }
  return sum;
}

function describeAbilities() {
  var parts = [];
  for (var i = 0; i < ABILITIES.length; i++) {
    var a = ABILITIES[i];
    var sc = state.abilities[a.key];
    var mod = Math.floor((sc - 10) / 2);
    parts.push(a.key + " " + sc + "(" + (mod >= 0 ? "+" : "") + mod + ")");
  }
  return parts.join("  ");
}

/* ------------------------------------------------------------- rendering */

function renderClassList() {
  var list = byId("class-list");
  if (!list) return;
  clearNode(list);
  for (var i = 0; i < CLASSES.length; i++) {
    var c = CLASSES[i];
    var active = state.selected === c.key ? " active" : "";
    var item = make("div", "class-item" + active);
    var seal = make("span", "seal");
    seal.innerHTML = SEALS[c.key] || "";
    var info = make("span", "class-info");
    info.appendChild(mk("span", "class-name", "class.dndclasses." + c.key));
    info.appendChild(mk("span", "class-en", "class.dndclasses." + c.key + ".subtitle"));
    var die = make("span", "class-die", c.hitDie);
    item.appendChild(seal);
    item.appendChild(info);
    item.appendChild(die);
    bindClassItem(item, c.key);
    list.appendChild(item);
  }
}

function bindClassItem(item, key) {
  item.addEventListener("click", function () { selectClass(key); });
}

function selectClass(key) {
  state.selected = key;
  state.equipment = 0;
  state.tab = 0;
  state.subclass = 0;
  state.subclassChosen = null;
  renderClassList();
  renderDetail();
  renderAbility();
  updateFooterSummary();
  refreshConfirmState();
  setMsg("", false);
}

function sectionLabel(key) {
  return mk("div", "section-label", key);
}

function runeRow(items) {
  var row = make("div", "rune-row");
  for (var i = 0; i < items.length; i++) {
    row.appendChild(mk("span", "rune", items[i]));
  }
  return row;
}

function statBox(labelKey, value) {
  var box = make("div", "stat");
  box.appendChild(mk("div", "stat-label", labelKey));
  if (value && typeof value === "object" && value.nodeType) box.appendChild(value);
  else box.appendChild(make("div", "stat-value", value));
  return box;
}

function renderDetail() {
  var header = byId("detail-header");
  var body = byId("detail-body");
  if (!header || !body) return;
  clearNode(body);
  if (!state.selected) {
    header.innerHTML = t("screen.dndclasses.creation.detail_title");
    var empty = mk("div", "empty-state", "screen.dndclasses.creation.detail_empty");
    body.appendChild(empty);
    return;
  }

  var c = findClass(state.selected);
  header.innerHTML = t("class.dndclasses." + c.key) + t("class.dndclasses." + c.key + ".subtitle_inline");

  var hero = make("div", "detail-hero");
  var heroSeal = make("span", "seal seal-lg");
  heroSeal.innerHTML = SEALS[c.key] || "";
  hero.appendChild(heroSeal);
  var heroText = make("div", null, null);
  heroText.appendChild(mk("div", "detail-name", "class.dndclasses." + c.key));
  var enLine = make("div", "detail-en", null);
  enLine.innerHTML = t("class.dndclasses." + c.key + ".subtitle");
  heroText.appendChild(enLine);
  hero.appendChild(heroText);
  body.appendChild(hero);

  body.appendChild(mk("p", "detail-desc", "class.dndclasses." + c.key + ".desc"));

  var stats = make("div", "stat-grid");
  stats.appendChild(statBox("screen.dndclasses.creation.hit_die", c.hitDie));
  stats.appendChild(statBox("screen.dndclasses.creation.primary", mk("div", "stat-value", c.primaryKey)));
  stats.appendChild(statBox("screen.dndclasses.creation.saves", runeRow(c.saves)));
  body.appendChild(stats);

  var tabsBar = make("div", "tabs detail-tabs");
  for (var t = 0; t < TABS.length; t++) {
    bindTabButton(tabsBar, t, TABS[t]);
  }
  body.appendChild(tabsBar);

  body.appendChild(renderActivePane(c));
}

function bindTabButton(bar, index, labelKey) {
  var active = state.tab === index ? " active" : "";
  var btn = mk("button", "tab" + active, labelKey);
  btn.setAttribute("type", "button");
  btn.addEventListener("click", function () { state.tab = index; renderDetail(); });
  bar.appendChild(btn);
}

function renderActivePane(c) {
  if (state.tab === 0) return paneOverview(c);
  if (state.tab === 1) return paneLevels(c);
  if (state.tab === 2) return paneSubclasses(c);
  return paneEquipment(c);
}

function prefixKeys(prefix, arr) {
  var out = [];
  for (var i = 0; i < arr.length; i++) out.push(prefix + arr[i]);
  return out;
}

function paneOverview(c) {
  var pane = make("div", "tab-pane");
  pane.appendChild(sectionLabel("screen.dndclasses.creation.armor_prof"));
  pane.appendChild(runeRow(prefixKeys("prof.dndclasses.", c.armor)));
  pane.appendChild(sectionLabel("screen.dndclasses.creation.weapon_prof"));
  pane.appendChild(runeRow(prefixKeys("prof.dndclasses.", c.weapons)));
  pane.appendChild(sectionLabel("screen.dndclasses.creation.saves"));
  pane.appendChild(runeRow(prefixKeys("ability.dndclasses.", c.saves)));
  return pane;
}

function slugify(value) {
  return String(value)
    .toLowerCase()
    .replace(/'/g, "")
    .replace(/[^a-z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "");
}

function featureBlock(item) {
  var box = make("div", "feature");
  var key = "feature.dndclasses." + slugify(item.e || item.n);
  var title = make("div", "feature-title");
  var name = make("span", "feature-name", null);
  name.innerHTML = t(key) + t(key + ".subtitle");
  title.appendChild(name);
  box.appendChild(title);
  var text = make("div", "feature-text", null);
  if (item.t) text.textContent = item.t;
  else text.innerHTML = t("feature.dndclasses.desc_placeholder");
  box.appendChild(text);
  return box;
}

function levelRow(level, items) {
  var row = make("div", "level-row" + (items.length ? "" : " empty"));
  var head = make("div", "level-head");
  head.appendChild(make("span", "level-badge", "Lv " + level));
  var prof = make("span", "level-prof", null);
  prof.innerHTML = t("screen.dndclasses.creation.level_prof") + profBonus(level);
  head.appendChild(prof);
  row.appendChild(head);
  var list = make("div", "level-items");
  if (!items.length) {
    list.appendChild(make("div", "level-none", "鈥?));
  } else {
    for (var i = 0; i < items.length; i++) {
      list.appendChild(featureBlock(items[i]));
    }
  }
  row.appendChild(list);
  return row;
}

function itemsAtLevel(levels, level) {
  if (!levels) return [];
  for (var i = 0; i < levels.length; i++) {
    if (levels[i].lv === level) return levels[i].items;
  }
  return [];
}

function paneLevels(c) {
  var pane = make("div", "tab-pane");
  var data = CLASS_FEATURES[c.key];
  if (!data) {
    pane.appendChild(mk("div", "empty-state", "screen.dndclasses.creation.no_level_data"));
    return pane;
  }
  if (data.intro) {
    pane.appendChild(sectionLabel("screen.dndclasses.creation.class_intro"));
    pane.appendChild(make("div", "level-intro", data.intro));
  }
  pane.appendChild(sectionLabel("screen.dndclasses.creation.level_features"));
  for (var lv = 1; lv <= 20; lv++) {
    pane.appendChild(levelRow(lv, itemsAtLevel(data.levels, lv)));
  }
  return pane;
}

function paneSubclasses(c) {
  var pane = make("div", "tab-pane");
  var data = CLASS_FEATURES[c.key];
  var meta = subclassMeta(c);
  if (!data || !meta.ids || meta.ids.length === 0) {
    pane.appendChild(mk("div", "empty-state", "screen.dndclasses.creation.no_subclass_data"));
    return pane;
  }
  var selectable = meta.level <= 1;

  var hint = make("div", "subclass-hint", null);
  if (selectable) {
    hint.innerHTML = t("screen.dndclasses.creation.subclass_select");
  } else {
    hint.innerHTML = t("screen.dndclasses.creation.subclass_preview")
      + meta.level + t("screen.dndclasses.creation.subclass_preview_suffix");
  }
  pane.appendChild(hint);

  var bar = make("div", "subclass-list");
  for (var i = 0; i < meta.ids.length; i++) {
    bindSubclassButton(bar, i, subclassNameKey(c, i), selectable);
  }
  pane.appendChild(bar);

  var count = data.subclasses ? data.subclasses.length : 0;
  var idx = state.subclass < count ? state.subclass : 0;
  var sc = data.subclasses ? data.subclasses[idx] : null;
  pane.appendChild(mk("div", "subclass-intro", subclassNameKey(c, idx)));
  if (sc && sc.intro) {
    pane.appendChild(make("div", "level-intro", sc.intro));
  }
  if (sc) {
    for (var lv = 1; lv <= 20; lv++) {
      var items = itemsAtLevel(sc.features, lv);
      if (!items.length) continue;
      pane.appendChild(levelRow(lv, items));
    }
  }
  return pane;
}

function bindSubclassButton(bar, index, labelKey, selectable) {
  var active = state.subclass === index ? " active" : "";
  var chosen = selectable && state.subclassChosen === index ? " chosen" : "";
  var btn = mk("button", "subclass-tab" + active + chosen, labelKey);
  btn.setAttribute("type", "button");
  btn.addEventListener("click", function () {
    state.subclass = index;
    if (selectable) state.subclassChosen = index;
    renderDetail();
  });
  bar.appendChild(btn);
}

function chosenSubclassId(c) {
  var meta = subclassMeta(c);
  if (meta.level > 1 || state.subclassChosen === null) return "";
  if (!meta.ids || state.subclassChosen >= meta.ids.length) return "";
  return meta.ids[state.subclassChosen];
}

function paneEquipment(c) {
  var pane = make("div", "tab-pane");
  pane.appendChild(sectionLabel("screen.dndclasses.creation.choose_equipment"));
  for (var i = 0; i < c.equipment.length; i++) {
    bindEquipChoice(pane, c, i);
  }
  return pane;
}

function bindEquipChoice(pane, c, idx) {
  var choice = c.equipment[idx];
  var active = state.equipment === idx ? " active" : "";
  var row = make("div", "equip-choice" + active);
  row.appendChild(make("span", "equip-radio"));
  var body = make("div", "equip-body");
  body.appendChild(mk("div", "equip-label", "equip.dndclasses." + c.key + "." + idx));
  var items = make("div", "item-row");
  for (var j = 0; j < choice.items.length; j++) {
    var id = choice.items[j];
    var chip = make("div", "item-chip");
    chip.appendChild(make("div", "item-placeholder"));
    var nameSpan = make("span", "item-name", null);
    nameSpan.innerHTML = itemName(id);
    chip.appendChild(nameSpan);
    items.appendChild(chip);
  }
  body.appendChild(items);
  row.appendChild(body);
  row.addEventListener("click", function () { state.equipment = idx; renderDetail(); });
  pane.appendChild(row);
}

function renderAbility() {
  var body = byId("ability-body");
  if (!body) return;
  clearNode(body);
  var spent = totalCost();
  var remaining = BUDGET - spent;
  var c = state.selected ? findClass(state.selected) : null;
  var primary = c ? c.primaryAbilities : [];

  var line = make("div", "points-line");
  line.appendChild(mk("span", "points-label", "screen.dndclasses.creation.points_spent"));
  line.appendChild(make("span", "points-value", spent + " / " + BUDGET));
  body.appendChild(line);

  var progress = make("div", "progress");
  var bar = make("div", "progress-bar");
  bar.setAttribute("style", "width:" + Math.round(spent * 100 / BUDGET) + "%");
  progress.appendChild(bar);
  body.appendChild(progress);

  var costRow = make("div", "cost-row rune-row");
  var remainRune = make("span", "rune", null);
  remainRune.innerHTML = t("screen.dndclasses.creation.points_remaining") + remaining;
  costRow.appendChild(remainRune);
  costRow.appendChild(mk("span", "rune", "screen.dndclasses.creation.points_range"));
  body.appendChild(costRow);

  for (var i = 0; i < ABILITIES.length; i++) {
    bindAbilityRow(body, ABILITIES[i], remaining, primary);
  }

  body.appendChild(mk("div", "ability-caption", "screen.dndclasses.creation.ability_caption"));
}

function bindAbilityRow(body, a, remaining, primary) {
  var sc = state.abilities[a.key];
  var bonus = (DND_RACE && DND_RACE.bonuses) ? (DND_RACE.bonuses[a.key] || 0) : 0;
  var finalScore = Math.min(20, sc + bonus);
  var mod = Math.floor((finalScore - 10) / 2);
  var canPlus = sc < MAX_SCORE && remaining >= (scoreCost(sc + 1) - scoreCost(sc));
  var canMinus = sc > MIN_SCORE;
  var isPrimary = primary.indexOf(a.key) >= 0;

  var row = make("div", "ability-row" + (isPrimary ? " primary" : ""));
  var minus = make("button", "pip-btn", "-");
  minus.setAttribute("type", "button");
  if (!canMinus) minus.setAttribute("disabled", "disabled");
  var score = make("span", "ability-score", bonus
    ? String(finalScore) + " (" + (bonus > 0 ? "+" : "") + bonus + ")"
    : String(finalScore));
  var plus = make("button", "pip-btn", "+");
  plus.setAttribute("type", "button");
  if (!canPlus) plus.setAttribute("disabled", "disabled");
  row.appendChild(minus);
  row.appendChild(score);
  row.appendChild(plus);

  var abName = make("span", "ability-name", null);
  abName.innerHTML = abilName(a.key);
  row.appendChild(abName);
  row.appendChild(make("span", "ability-mod", (mod >= 0 ? "+" : "") + mod));

  minus.addEventListener("click", function () { changeAbility(a.key, -1); });
  plus.addEventListener("click", function () { changeAbility(a.key, 1); });
  body.appendChild(row);
}

function changeAbility(key, delta) {
  var sc = state.abilities[key];
  if (delta > 0) {
    if (sc >= MAX_SCORE) return;
    if (totalCost() + (scoreCost(sc + 1) - scoreCost(sc)) > BUDGET) return;
    state.abilities[key] = sc + 1;
  } else {
    if (sc <= MIN_SCORE) return;
    state.abilities[key] = sc - 1;
  }
  renderAbility();
  updateFooterSummary();
  refreshConfirmState();
  setMsg("", false);
}

/* --------------------------------------------------------------- helpers */

function updateFooterSummary() {
  var el = byId("footer-summary");
  if (!el) return;
  if (!state.selected) {
    el.innerHTML = t("screen.dndclasses.creation.footer_no_class");
    return;
  }
  var c = findClass(state.selected);
  var remaining = BUDGET - totalCost();
  el.innerHTML = t("screen.dndclasses.creation.footer_selected") + t("class.dndclasses." + c.key)
    + " (" + c.hitDie + ")  " + t("screen.dndclasses.creation.footer_remaining") + remaining;
}

function refreshConfirmState() {
  var btn = byId("btn-confirm");
  if (!btn) return;
  var raceChosen = DND_RACE;
  var ok = state.selected !== null && totalCost() === BUDGET && raceChosen;
  if (ok) btn.removeAttribute("disabled");
  else btn.setAttribute("disabled", "disabled");
}

function setMsg(key, ok) {
  var el = byId("confirm-msg");
  if (!el) return;
  el.innerHTML = key ? t(key) : "";
  if (ok) el.setAttribute("class", "confirm-msg ok");
  else el.setAttribute("class", "confirm-msg");
}

/* ---------------------------------------------------------------- modal */

function onConfirm() {
  if (!DND_RACE) {
    setMsg("screen.dndclasses.race.required", false);
    return;
  }
  if (!state.selected) { setMsg("screen.dndclasses.creation.msg_no_class", false); return; }
  if (totalCost() !== BUDGET) { setMsg("screen.dndclasses.creation.msg_budget", false); return; }
  setMsg("", false);
  openSummary();
}

function summaryLine(keyKey, valueMarkup) {
  var line = make("div", "summary-line");
  line.appendChild(mk("span", "summary-key", keyKey));
  var val = make("span", "summary-val", null);
  val.innerHTML = valueMarkup;
  line.appendChild(val);
  return line;
}

function findAbility(key) {
  for (var i = 0; i < ABILITIES.length; i++) {
    if (ABILITIES[i].key === key) return ABILITIES[i];
  }
  return null;
}

function summaryAbilities() {
  var order = [["str", "con", "dex"], ["wis", "int", "cha"]];
  var wrap = make("div", "summary-abilities");
  for (var r = 0; r < order.length; r++) {
    var row = make("div", "summary-ability-row");
    for (var i = 0; i < order[r].length; i++) {
      var ab = findAbility(order[r][i]);
      var raceBonus = (DND_RACE && DND_RACE.bonuses) ? (DND_RACE.bonuses[ab.key] || 0) : 0;
      var sc = Math.min(20, state.abilities[ab.key] + raceBonus);
      var mod = Math.floor((sc - 10) / 2);
      var cell = make("div", "summary-ability");
      cell.appendChild(mk("div", "summary-ability-name", "ability.dndclasses." + ab.key));
      cell.appendChild(make("div", "summary-ability-score", sc + " (" + (mod >= 0 ? "+" : "") + mod + ")"));
      row.appendChild(cell);
    }
    wrap.appendChild(row);
  }
  return wrap;
}

function openSummary() {
  var c = findClass(state.selected);
  var choice = c.equipment[state.equipment];
  var body = byId("sheet-body");
  if (!body) return;
  clearNode(body);
  body.appendChild(summaryLine("screen.dndclasses.creation.summary_class",
    t("class.dndclasses." + c.key) + t("class.dndclasses." + c.key + ".subtitle_inline")));
  body.appendChild(summaryLine("screen.dndclasses.creation.hit_die", c.hitDie));
  body.appendChild(summaryLine("screen.dndclasses.creation.primary", t(c.primaryKey)));
  body.appendChild(summaryLine("screen.dndclasses.creation.saves",
    prefixKeys("ability.dndclasses.", c.saves).map(function (k) { return t(k); }).join(" / ")));
  var subId = chosenSubclassId(c);
  if (subId) {
    body.appendChild(summaryLine("screen.dndclasses.creation.summary_subclass", subclassNameKey(c, state.subclassChosen)));
  }
  body.appendChild(mk("div", "summary-key summary-abilities-label", "screen.dndclasses.creation.abilities"));
  body.appendChild(summaryAbilities());
  body.appendChild(summaryLine("screen.dndclasses.creation.summary_equipment",
    t("equip.dndclasses." + c.key + "." + state.equipment)));
  var overlay = byId("sheet-overlay");
  if (overlay) overlay.setAttribute("class", "sheet-overlay open");
}

function closeSummary() {
  var overlay = byId("sheet-overlay");
  if (overlay) overlay.setAttribute("class", "sheet-overlay");
}

function commitCharacter() {
  var c = findClass(state.selected);
  var choice = c.equipment[state.equipment];
  var race = DND_RACE;
  var payload = {
    race: race ? race.race : "",
    subrace: race ? race.subrace : "",
    raceBonuses: (race && race.bonuses) ? race.bonuses : {},
    class: state.selected,
    className: c.key,
    subclass: chosenSubclassId(c),
    abilities: state.abilities,
    pointsSpent: totalCost(),
    equipmentChoice: state.equipment,
    equipmentLabel: "equip.dndclasses." + c.key + "." + state.equipment,
    equipment: choice.items
  };

  var payloadInput = byId("class-payload");
  if (payloadInput) payloadInput.value = JSON.stringify(payload);

  console.log("[dndClasses] character payload " + JSON.stringify(payload));
  closeSummary();
  setMsg("screen.dndclasses.creation.msg_submitted", true);
}

/* ------------------------------------------------------------- sheet mode */

var pageMode = "";
var lastStateValue = "";

function setHidden(el, hidden) {
  if (!el) return;
  var cls = el.getAttribute("class") || "";
  if (hidden) {
    if (cls.indexOf("hidden") < 0) el.setAttribute("class", cls + " hidden");
  } else {
    var parts = cls.split(" ");
    var out = "";
    for (var i = 0; i < parts.length; i++) {
      if (parts[i] && parts[i] !== "hidden") out += (out ? " " : "") + parts[i];
    }
    el.setAttribute("class", out);
  }
}

function subclassIndexByKey(c, key) {
  if (!key) return -1;
  var meta = subclassMeta(c);
  if (!meta.ids) return -1;
  for (var i = 0; i < meta.ids.length; i++) {
    if (meta.ids[i] === key) return i;
  }
  return -1;
}

function subclassNameByKey(c, key) {
  var idx = subclassIndexByKey(c, key);
  return idx >= 0 ? subclassNameKey(c, idx) : "";
}

function findSubclassByKey(c, key) {
  var idx = subclassIndexByKey(c, key);
  var data = CLASS_FEATURES[c.key];
  if (idx < 0 || !data || !data.subclasses || idx >= data.subclasses.length) return null;
  return data.subclasses[idx];
}

function sheetAbilities(abilities) {
  var order = [["str", "con", "dex"], ["wis", "int", "cha"]];
  var wrap = make("div", "summary-abilities");
  for (var r = 0; r < order.length; r++) {
    var row = make("div", "summary-ability-row");
    for (var i = 0; i < order[r].length; i++) {
      var ab = findAbility(order[r][i]);
      var sc = abilities && abilities[ab.key] !== undefined ? abilities[ab.key] : 8;
      var mod = Math.floor((sc - 10) / 2);
      var cell = make("div", "summary-ability");
      cell.appendChild(mk("div", "summary-ability-name", "ability.dndclasses." + ab.key));
      cell.appendChild(make("div", "summary-ability-score", sc + " (" + (mod >= 0 ? "+" : "") + mod + ")"));
      row.appendChild(cell);
    }
    wrap.appendChild(row);
  }
  return wrap;
}

function expBlock(data) {
  var level = data.level ? data.level : 1;
  var exp = data.exp ? data.exp : 0;
  var start = data.expLevelStart ? data.expLevelStart : 0;
  var next = data.expToNext ? data.expToNext : 0;
  var wrap = make("div", "exp-block");

  var line = make("div", "exp-line");
  line.appendChild(mk("span", "exp-label", "screen.dndclasses.sheet.experience"));

  var bar = make("div", "progress-bar");
  if (level >= 20) {
    var maxed = make("span", "exp-value", null);
    maxed.innerHTML = exp + " / " + t("screen.dndclasses.sheet.max_level");
    line.appendChild(maxed);
    bar.setAttribute("style", "width:100%");
  } else {
    line.appendChild(make("span", "exp-value", exp + " / " + next));
    var span = next - start;
    var into = exp - start;
    var pct = span > 0 ? Math.round(into * 100 / span) : 0;
    if (pct < 0) pct = 0;
    if (pct > 100) pct = 100;
    bar.setAttribute("style", "width:" + pct + "%");
  }

  var progress = make("div", "progress");
  progress.appendChild(bar);
  wrap.appendChild(line);
  wrap.appendChild(progress);
  return wrap;
}

function collectFeatures(c, level, subclassKey) {
  var groups = [];
  var data = CLASS_FEATURES[c.key];
  if (!data) return groups;
  var sub = findSubclassByKey(c, subclassKey);
  for (var lv = 1; lv <= level; lv++) {
    var items = itemsAtLevel(data.levels, lv).slice();
    if (sub) {
      var subItems = itemsAtLevel(sub.features, lv);
      for (var i = 0; i < subItems.length; i++) items.push(subItems[i]);
    }
    if (items.length) groups.push({ lv: lv, items: items });
  }
  return groups;
}

function sheetNum(v) {
  return (v === undefined || v === null) ? 0 : Math.round(v);
}

function sheetResources(data) {
  var grid = make("div", "stat-grid");
  grid.appendChild(statBox("screen.dndclasses.sheet.hp", sheetNum(data.hp) + " / " + sheetNum(data.hpMax)));
  grid.appendChild(statBox("screen.dndclasses.sheet.ac", String(data.ac === undefined ? 0 : data.ac)));
  grid.appendChild(statBox("screen.dndclasses.sheet.carry",
    String(data.weightCapacity === undefined ? 0 : data.weightCapacity)));
  return grid;
}

function sheetSlots(slots) {
  var row = make("div", "rune-row");
  var any = false;
  if (slots) {
    for (var ring in slots) {
      var s = slots[ring];
      if (!s) continue;
      any = true;
      var span = make("span", "rune", null);
      span.innerHTML = t("screen.dndclasses.sheet.slot_ring") + " " + ring + ": "
        + sheetNum(s.cur) + "/" + sheetNum(s.max);
      row.appendChild(span);
    }
  }
  if (!any) row.appendChild(make("span", "rune", "鈥?));
  return row;
}

function renderSheet(data) {
  var body = byId("character-sheet-body");
  var header = byId("sheet-header");
  if (!body) return;
  clearNode(body);
  var c = findClass(data["class"]);
  if (header) header.innerHTML = t("screen.dndclasses.sheet.character");

  var race = raceFind(data.race);
  if (race) {
    body.appendChild(sectionLabel("screen.dndclasses.sheet.race"));
    var raceBox = make("div", "race-sheet");
    var sub = raceFindSub(race, data.subrace);
    raceBox.appendChild(mk("div", "detail-name", "race.dndclasses." + race.key));
    if (sub) {
      raceBox.appendChild(mk("div", "detail-en", "race.dndclasses." + race.key + "." + sub.key));
    }
    var raceTraits = (sub && sub.traits) ? sub.traits : (race.traits || []);
    if (raceTraits.length) {
      var chips = make("div", "trait-list");
      for (var rt = 0; rt < raceTraits.length; rt++) {
        chips.appendChild(mk("span", "trait-chip", "trait.dndclasses." + raceTraits[rt]));
      }
      raceBox.appendChild(chips);
    }
    body.appendChild(raceBox);
  }

  if (!c) {
    body.appendChild(mk("div", "empty-state", "screen.dndclasses.sheet.unknown_class"));
  } else {
    var hero = make("div", "detail-hero");
    var heroSeal = make("span", "seal seal-lg");
    heroSeal.innerHTML = SEALS[c.key] || "";
    hero.appendChild(heroSeal);
    var ht = make("div");
    ht.appendChild(mk("div", "detail-name", "class.dndclasses." + c.key));
    var subName = subclassNameByKey(c, data.subclass);
    var enLine = make("div", "detail-en", null);
    enLine.innerHTML = t("class.dndclasses." + c.key + ".subtitle") + (subName ? (" 路 " + t(subName)) : "");
    ht.appendChild(enLine);
    hero.appendChild(ht);
    body.appendChild(hero);

    body.appendChild(mk("p", "detail-desc", "class.dndclasses." + c.key + ".desc"));

    var stats = make("div", "stat-grid");
    stats.appendChild(statBox("screen.dndclasses.sheet.level", "Lv " + (data.level ? data.level : 1)));
    stats.appendChild(statBox("screen.dndclasses.sheet.proficiency", "+" + (data.proficiency ? data.proficiency : 2)));
    stats.appendChild(statBox("screen.dndclasses.creation.hit_die", c.hitDie));
    body.appendChild(stats);

    body.appendChild(sectionLabel("screen.dndclasses.sheet.experience"));
    body.appendChild(expBlock(data));

    body.appendChild(sectionLabel("screen.dndclasses.creation.abilities"));
    body.appendChild(sheetAbilities(data.abilities));

    body.appendChild(sectionLabel("screen.dndclasses.sheet.resources"));
    body.appendChild(sheetResources(data));
    body.appendChild(sectionLabel("screen.dndclasses.sheet.slots"));
    body.appendChild(sheetSlots(data.slots));

    body.appendChild(sectionLabel("screen.dndclasses.sheet.gained_features"));
    var groups = collectFeatures(c, data.level ? data.level : 1, data.subclass);
    if (!groups.length) {
      body.appendChild(mk("div", "empty-state", "screen.dndclasses.sheet.no_features"));
    } else {
      for (var g = 0; g < groups.length; g++) {
        body.appendChild(levelRow(groups[g].lv, groups[g].items));
      }
    }
  }

  setHidden(byId("col-left"), true);
  setHidden(byId("col-mid"), true);
  setHidden(byId("col-right"), true);
  setHidden(byId("sheet-view"), false);
  setHidden(byId("grimoire-footer"), true);
  setHidden(byId("page-loading"), true);
}

function enterLoading() {
  setHidden(byId("col-left"), true);
  setHidden(byId("col-mid"), true);
  setHidden(byId("col-right"), true);
  setHidden(byId("sheet-view"), true);
  setHidden(byId("grimoire-footer"), true);
  setHidden(byId("page-loading"), false);
}

function showCreateView() {
  setHidden(byId("page-loading"), true);
  setHidden(byId("sheet-view"), true);
  setHidden(byId("col-left"), false);
  setHidden(byId("col-mid"), false);
  setHidden(byId("col-right"), false);
  setHidden(byId("grimoire-footer"), false);
}

function pollPageState() {
  var el = byId("page-state");
  if (!el) return;
  var value = el.value;
  if (!value || value === lastStateValue) return;
  var data;
  try {
    data = JSON.parse(value);
  } catch (err) {
    return;
  }
  if (!data || !data.mode) return;
  lastStateValue = value;
  if (data.mode === "sheet") {
    pageMode = "sheet";
    renderSheet(data);
  } else if (!pageMode) {
    pageMode = "create";
    showCreateView();
  }
}

/* ------------------------------------------------------------ race step */

var RACE_DATA = [
  { key: "dwarf", speed: 25, size: "medium", subraces: [
    { key: "hill_dwarf", asi: { con: 2, wis: 1 }, asiChoice: 0, speed: 25,
      traits: ["darkvision_60", "dwarf_resilience", "stonecunning", "dwarf_combat_training", "tool_proficiency"] },
    { key: "mountain_dwarf", asi: { con: 2, str: 2 }, asiChoice: 0, speed: 25,
      traits: ["darkvision_60", "dwarf_resilience", "stonecunning", "dwarf_combat_training", "dwarf_armor_training"] }
  ]},
  { key: "elf", speed: 30, size: "medium", subraces: [
    { key: "high_elf", asi: { dex: 2, int: 1 }, asiChoice: 0, speed: 30,
      traits: ["darkvision_60", "keen_senses", "fey_ancestry", "trance", "elf_weapon_training", "high_elf_cantrip"] },
    { key: "wood_elf", asi: { dex: 2, wis: 1 }, asiChoice: 0, speed: 35,
      traits: ["darkvision_60", "keen_senses", "fey_ancestry", "trance", "elf_weapon_training", "fleet_of_foot", "mask_of_the_wild"] },
    { key: "dark_elf", asi: { dex: 2, cha: 1 }, asiChoice: 0, speed: 30,
      traits: ["superior_darkvision_120", "sunlight_sensitivity", "drow_magic", "keen_senses", "fey_ancestry", "trance"] }
  ]},
  { key: "halfling", speed: 25, size: "small", subraces: [
    { key: "lightfoot", asi: { dex: 2, cha: 1 }, asiChoice: 0, speed: 25,
      traits: ["lucky", "brave", "halfling_nimbleness"] },
    { key: "stout", asi: { dex: 2, con: 1 }, asiChoice: 0, speed: 25,
      traits: ["lucky", "brave", "halfling_nimbleness", "stout_resilience"] }
  ]},
  { key: "human", speed: 30, size: "medium", subraces: [
    { key: "standard_human", asi: { str: 1, dex: 1, con: 1, int: 1, wis: 1, cha: 1 }, asiChoice: 0, speed: 30, traits: [] },
    { key: "variant_human", asi: {}, asiChoice: 2, speed: 30, traits: ["skill_versatility", "feat"] }
  ]},
  { key: "dragonborn", speed: 30, size: "medium", subraces: [
    { key: "dragonborn", asi: { str: 2, cha: 1 }, asiChoice: 0, speed: 30,
      traits: ["draconic_ancestry", "breath_weapon", "damage_resistance"] }
  ]},
  { key: "gnome", speed: 25, size: "small", subraces: [
    { key: "forest_gnome", asi: { int: 2, dex: 1 }, asiChoice: 0, speed: 25,
      traits: ["darkvision_60", "gnome_cunning", "natural_illusionist"] },
    { key: "rock_gnome", asi: { int: 2, con: 1 }, asiChoice: 0, speed: 25,
      traits: ["darkvision_60", "gnome_cunning", "artificers_lore", "tinker"] }
  ]},
  { key: "half_elf", speed: 30, size: "medium", subraces: [
    { key: "half_elf", asi: { cha: 2 }, asiChoice: 2, speed: 30,
      traits: ["darkvision_60", "fey_ancestry", "skill_versatility"] }
  ]},
  { key: "half_orc", speed: 30, size: "medium", subraces: [
    { key: "half_orc", asi: { str: 2, con: 1 }, asiChoice: 0, speed: 30,
      traits: ["darkvision_60", "menacing", "relentless_endurance", "savage_attacks"] }
  ]},
  { key: "tiefling", speed: 30, size: "medium", subraces: [
    { key: "tiefling", asi: { cha: 2, int: 1 }, asiChoice: 0, speed: 30,
      traits: ["darkvision_60", "hellish_resistance", "infernal_legacy"] }
  ]}
];
(function () {
  var colors = ["black", "blue", "brass", "bronze", "copper", "gold", "green", "red", "silver", "white"];
  for (var i = 0; i < colors.length; i++) {
    RACE_DATA[4].subraces.push({
      key: colors[i], asi: { str: 2, cha: 1 }, asiChoice: 0, speed: 30,
      traits: ["draconic_ancestry", "breath_weapon", "damage_resistance", "draconic_" + colors[i]]
    });
  }
})();

var raceState = { race: null, subrace: null, picks: [] };
var raceBound = false;

function raceFind(key) {
  for (var i = 0; i < RACE_DATA.length; i++) {
    if (RACE_DATA[i].key === key) return RACE_DATA[i];
  }
  return null;
}

function raceFindSub(race, key) {
  if (!race) return null;
  for (var i = 0; i < race.subraces.length; i++) {
    if (race.subraces[i].key === key) return race.subraces[i];
  }
  return null;
}

function racePattern(sub) {
  var values = [];
  if (!sub) return values;
  var keys = Object.keys(sub.asi || {});
  for (var i = 0; i < keys.length; i++) values.push(sub.asi[keys[i]]);
  for (var j = 0; j < (sub.asiChoice || 0); j++) values.push(1);
  values.sort(function (a, b) { return b - a; });
  return values;
}

function raceResetPicks(sub) {
  var values = racePattern(sub);
  raceState.picks = [];
  for (var i = 0; i < values.length; i++) raceState.picks.push(null);
  if (values.length === 6) {
    for (var k = 0; k < ABILITIES.length; k++) raceState.picks[k] = ABILITIES[k].key;
  }
}

function raceShowStep(isRace) {
  var raceList = byId("race-list");
  var raceDetail = byId("race-detail");
  var classList = byId("class-list");
  var classDetail = byId("detail-body");
  var footer = byId("race-footer");
  var stepRace = byId("step-race");
  var stepClass = byId("step-class");
  if (!raceList || !raceDetail || !classList || !classDetail) return;
  if (isRace) {
    raceList.setAttribute("class", "race-list");
    raceDetail.setAttribute("class", "race-detail");
    classList.setAttribute("class", "class-list hidden");
    classDetail.setAttribute("class", "hidden");
    if (footer) footer.setAttribute("class", "card-footer");
    if (stepRace) stepRace.setAttribute("class", "step-tab active");
    if (stepClass) stepClass.setAttribute("class", "step-tab");
    var header = byId("detail-header");
    if (header) header.innerHTML = t("screen.dndclasses.race.title");
  } else {
    raceList.setAttribute("class", "race-list hidden");
    raceDetail.setAttribute("class", "race-detail hidden");
    classList.setAttribute("class", "class-list");
    classDetail.setAttribute("class", "");
    if (footer) footer.setAttribute("class", "card-footer hidden");
    if (stepRace) stepRace.setAttribute("class", "step-tab");
    if (stepClass) stepClass.setAttribute("class", "step-tab active");
    var classHeader = byId("detail-header");
    if (classHeader) classHeader.innerHTML = t("screen.dndclasses.creation.detail_title");
  }
}

function raceRenderList() {
  var list = byId("race-list");
  if (!list) return;
  clearNode(list);
  for (var i = 0; i < RACE_DATA.length; i++) {
    (function (race) {
      var btn = make("button", "race-item", null);
      btn.setAttribute("type", "button");
      if (raceState.race === race.key) btn.setAttribute("class", "race-item active");
      btn.innerHTML = t("race.dndclasses." + race.key);
      btn.addEventListener("click", function () {
        raceState.race = race.key;
        raceState.subrace = race.subraces[0].key;
        raceResetPicks(race.subraces[0]);
        raceRenderList();
        raceRenderDetail();
        raceRefreshConfirm();
      });
      list.appendChild(btn);
    })(RACE_DATA[i]);
  }
}

function raceRenderDetail() {
  var detail = byId("race-detail");
  if (!detail) return;
  clearNode(detail);
  var race = raceFind(raceState.race);
  if (!race) {
    detail.appendChild(mk("p", "choice-sub", "screen.dndclasses.race.select_race"));
    return;
  }
  detail.appendChild(mk("div", "card-title", "race.dndclasses." + race.key));
  var infoLine = make("div", "card-sub", null);
  infoLine.innerHTML = t("screen.dndclasses.race.speed") + " " + race.speed + " ft 路 "
    + t("size.dndclasses." + race.size);
  detail.appendChild(infoLine);

  var subRow = make("div", "subrace-row");
  for (var i = 0; i < race.subraces.length; i++) {
    (function (sub) {
      var btn = mk("button", "subrace-btn", "race.dndclasses." + race.key + "." + sub.key);
      btn.setAttribute("type", "button");
      if (raceState.subrace === sub.key) btn.setAttribute("class", "subrace-btn active");
      btn.addEventListener("click", function () {
        raceState.subrace = sub.key;
        raceResetPicks(sub);
        raceRenderDetail();
        raceRefreshConfirm();
      });
      subRow.appendChild(btn);
    })(race.subraces[i]);
  }
  detail.appendChild(subRow);

  var sub = raceFindSub(race, raceState.subrace);
  if (!sub) return;

  var values = racePattern(sub);
  var parts = [];
  for (var v = 0; v < values.length; v++) parts.push("+" + values[v]);
  var patternLine = make("div", "card-sub", null);
  patternLine.innerHTML = t("screen.dndclasses.race.bonus_pattern") + " " + parts.join(" / ");
  detail.appendChild(patternLine);

  if (values.length < 6) {
    detail.appendChild(mk("div", "card-sub", "screen.dndclasses.race.assign_hint"));
    for (var slot = 0; slot < values.length; slot++) {
      (function (slotIndex, bonusValue) {
        var row = make("div", "asi-row");
        row.appendChild(make("span", "rune", "+" + bonusValue + ":"));
        for (var a = 0; a < ABILITIES.length; a++) {
          (function (ability) {
            var selected = raceState.picks[slotIndex] === ability.key;
            var usedElsewhere = false;
            for (var p = 0; p < raceState.picks.length; p++) {
              if (p !== slotIndex && raceState.picks[p] === ability.key) usedElsewhere = true;
            }
            var btn = make("button", "asi-btn", null);
            btn.innerHTML = abilName(ability.key);
            btn.setAttribute("type", "button");
            if (selected) btn.setAttribute("class", "asi-btn active");
            if (usedElsewhere) btn.setAttribute("class", "asi-btn used");
            btn.addEventListener("click", function () {
              if (usedElsewhere) {
                var duplicateMsg = byId("race-msg");
                if (duplicateMsg) duplicateMsg.innerHTML = t("screen.dndclasses.race.duplicate");
                return;
              }
              raceState.picks[slotIndex] = ability.key;
              raceRenderDetail();
              raceRefreshConfirm();
            });
            row.appendChild(btn);
          })(ABILITIES[a]);
        }
        detail.appendChild(row);
      })(slot, values[slot]);
    }
  }

  var traits = make("div", "trait-list");
  for (var k = 0; k < sub.traits.length; k++) {
    var chip = make("span", "trait-chip", null);
    chip.innerHTML = t("trait.dndclasses." + sub.traits[k]);
    traits.appendChild(chip);
  }
  detail.appendChild(traits);
}

function raceAllPicked() {
  for (var i = 0; i < raceState.picks.length; i++) {
    if (!raceState.picks[i]) return false;
  }
  return true;
}

function raceRefreshConfirm() {
  var confirm = byId("race-confirm");
  if (!confirm) return;
  var sub = raceFindSub(raceFind(raceState.race), raceState.subrace);
  if (sub) confirm.removeAttribute("disabled");
  else confirm.setAttribute("disabled", "disabled");
}

function raceBonuses() {
  var result = {};
  var sub = raceFindSub(raceFind(raceState.race), raceState.subrace);
  var values = racePattern(sub);
  for (var i = 0; i < values.length; i++) {
    if (raceState.picks[i]) result[raceState.picks[i]] = values[i];
  }
  return result;
}

function initRace() {
  var raceConfirm = byId("race-confirm");
  var raceList = byId("race-list");
  if (!raceConfirm || !raceList) return;
  if (!raceBound) {
    raceBound = true;
    raceConfirm.addEventListener("click", function () {
      var race = raceFind(raceState.race);
      var sub = raceFindSub(race, raceState.subrace);
      if (!race || !sub) return;
      if (!raceAllPicked()) {
        var msg = byId("race-msg");
        if (msg) msg.innerHTML = t("screen.dndclasses.race.assign_incomplete");
        return;
      }
      DND_RACE = { race: race.key, subrace: sub.key, bonuses: raceBonuses() };
      raceShowStep(false);
      dndClassRefresh();
    });
    var stepRace = byId("step-race");
    var stepClass = byId("step-class");
    if (stepRace) {
      stepRace.addEventListener("click", function () { raceShowStep(true); });
    }
    if (stepClass) {
      stepClass.addEventListener("click", function () {
        if (!DND_RACE) {
          var msg = byId("confirm-msg");
          if (msg) msg.innerHTML = t("screen.dndclasses.race.required");
          return;
        }
        raceShowStep(false);
      });
    }
  }
  DND_RACE = null;
  raceState.race = null;
  raceState.subrace = null;
  raceState.picks = [];
  raceRenderList();
  raceRenderDetail();
  raceRefreshConfirm();
  raceShowStep(true);
}

/* ----------------------------------------------------------------- init */

function init() {
  var confirm = byId("btn-confirm");
  if (confirm) confirm.addEventListener("click", onConfirm);
  var cancel = byId("sheet-cancel");
  if (cancel) cancel.addEventListener("click", closeSummary);
  var commit = byId("sheet-commit");
  if (commit) commit.addEventListener("click", commitCharacter);

  initRace();
  renderClassList();
  renderDetail();
  renderAbility();
  updateFooterSummary();
  refreshConfirmState();

  enterLoading();
  pollPageState();
  if (typeof setInterval === "function") {
    setInterval(pollPageState, 150);
  }
  if (typeof setTimeout === "function") {
    setTimeout(function () {
      if (!pageMode) {
        pageMode = "create";
        showCreateView();
      }
    }, 1500);
  }
}

var pageStarted = false;

function dndClassRefresh() {
  renderAbility();
  updateFooterSummary();
  refreshConfirmState();
}

function boot() {
  if (pageStarted) return;
  pageStarted = true;
  init();
}

document.addEventListener("DOMContentLoaded", boot);
if (document.readyState === "complete" || document.readyState === "interactive") {
  boot();
}
