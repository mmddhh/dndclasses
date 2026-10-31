(function () {
  function tr(key) {
    return "<translation>" + key + "</translation>";
  }

  // Mirrors RaceData (PHB bonuses become a free point pool; the player assigns them in the
  // ability panel). The server validates the total against its own copy.
  var BUILTIN = [
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
  var colors = ["black", "blue", "brass", "bronze", "copper", "gold", "green", "red", "silver", "white"];
  for (var c = 0; c < colors.length; c++) {
    BUILTIN[4].subraces.push({
      key: colors[c],
      asi: { str: 2, cha: 1 },
      asiChoice: 0,
      speed: 30,
      traits: ["draconic_ancestry", "breath_weapon", "damage_resistance", "draconic_" + colors[c]]
    });
  }

  var listEl = document.getElementById("race-list");
  var detailEl = document.getElementById("race-detail");
  var confirmEl = document.getElementById("race-confirm");
  var msgEl = document.getElementById("race-msg");
  var overlay = document.getElementById("race-overlay");
  if (!listEl || !detailEl || !confirmEl || !overlay) return;

  var races = BUILTIN;
  var state = { race: null, subrace: null };

  function findRace(key) {
    for (var i = 0; i < races.length; i++) {
      if (races[i].key === key) return races[i];
    }
    return null;
  }

  function findSubrace(race, key) {
    if (!race) return null;
    for (var i = 0; i < race.subraces.length; i++) {
      if (race.subraces[i].key === key) return race.subraces[i];
    }
    return null;
  }

  function points(subrace) {
    if (!subrace) return 0;
    var total = subrace.asiChoice || 0;
    var keys = Object.keys(subrace.asi || {});
    for (var i = 0; i < keys.length; i++) {
      total += subrace.asi[keys[i]];
    }
    return total;
  }

  function raceName(key) {
    return tr("race.dndclasses." + key);
  }

  function subraceName(raceKey, key) {
    return tr("race.dndclasses." + raceKey + "." + key);
  }

  function traitName(key) {
    return tr("trait.dndclasses." + key);
  }

  function renderDetail() {
    detailEl.innerHTML = "";
    var race = findRace(state.race);
    if (!race) {
      detailEl.innerHTML = "<p class='choice-sub'>" + tr("screen.dndclasses.race.select_race") + "</p>";
      return;
    }
    var title = document.createElement("div");
    title.setAttribute("class", "card-title");
    title.innerHTML = raceName(race.key);
    detailEl.appendChild(title);

    var info = document.createElement("div");
    info.setAttribute("class", "card-sub");
    info.innerHTML = tr("screen.dndclasses.race.speed") + " " + race.speed + " ft · "
      + tr("size.dndclasses." + race.size);
    detailEl.appendChild(info);

    var subRow = document.createElement("div");
    subRow.setAttribute("class", "subrace-row");
    for (var i = 0; i < race.subraces.length; i++) {
      (function (sub) {
        var btn = document.createElement("button");
        btn.setAttribute("type", "button");
        btn.setAttribute("class", "subrace-btn" + (state.subrace === sub.key ? " active" : ""));
        btn.innerHTML = subraceName(race.key, sub.key);
        btn.addEventListener("click", function () {
          state.subrace = sub.key;
          renderDetail();
          refreshConfirm();
        });
        subRow.appendChild(btn);
      })(race.subraces[i]);
    }
    detailEl.appendChild(subRow);

    var sub = findSubrace(race, state.subrace);
    if (!sub) return;

    var asi = document.createElement("div");
    asi.setAttribute("class", "card-sub");
    asi.innerHTML = tr("screen.dndclasses.race.free_points") + " +" + points(sub)
      + "（" + tr("screen.dndclasses.race.assign_hint") + "）";
    detailEl.appendChild(asi);

    var traits = document.createElement("div");
    traits.setAttribute("class", "trait-list");
    for (var k = 0; k < sub.traits.length; k++) {
      var chip = document.createElement("span");
      chip.setAttribute("class", "trait-chip");
      chip.innerHTML = traitName(sub.traits[k]);
      traits.appendChild(chip);
    }
    detailEl.appendChild(traits);
  }

  function renderList() {
    listEl.innerHTML = "";
    for (var i = 0; i < races.length; i++) {
      (function (race) {
        var btn = document.createElement("button");
        btn.setAttribute("type", "button");
        btn.setAttribute("class", "race-item" + (state.race === race.key ? " active" : ""));
        btn.innerHTML = raceName(race.key);
        btn.addEventListener("click", function () {
          state.race = race.key;
          state.subrace = race.subraces[0].key;
          renderList();
          renderDetail();
          refreshConfirm();
        });
        listEl.appendChild(btn);
      })(races[i]);
    }
  }

  function refreshConfirm() {
    var race = findRace(state.race);
    var sub = findSubrace(race, state.subrace);
    var ok = race && sub;
    if (ok) confirmEl.removeAttribute("disabled");
    else confirmEl.setAttribute("disabled", "disabled");
  }

  confirmEl.addEventListener("click", function () {
    var race = findRace(state.race);
    var sub = findSubrace(race, state.subrace);
    if (!race || !sub) return;
    window.dndRace = {
      race: race.key,
      subrace: sub.key,
      points: points(sub)
    };
    overlay.setAttribute("class", "race-overlay hidden");
    if (typeof window.dndClassRefresh === "function") {
      window.dndClassRefresh();
    }
  });

  renderList();
  renderDetail();
  refreshConfirm();

  function syncMode() {
    try {
      var stateEl = document.getElementById("page-state");
      if (!stateEl || !stateEl.value) return;
      var mode = JSON.parse(stateEl.value).mode;
      if (mode && mode !== "create") {
        overlay.setAttribute("class", "race-overlay hidden");
      }
    } catch (err) {
    }
  }
  setInterval(syncMode, 300);
})();
