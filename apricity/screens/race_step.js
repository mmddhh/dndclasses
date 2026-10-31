(function () {
  function tr(key) {
    return "<translation>" + key + "</translation>";
  }

  // Mirrors RaceData: `asi`/`asiChoice` are the PHB bonuses; the player only chooses which
  // abilities receive those values (the values themselves are fixed).
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
  var abilities = ["str", "dex", "con", "int", "wis", "cha"];
  var state = { race: null, subrace: null, picks: [] };

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

  function pattern(subrace) {
    var values = [];
    if (!subrace) return values;
    var keys = Object.keys(subrace.asi || {});
    for (var i = 0; i < keys.length; i++) {
      values.push(subrace.asi[keys[i]]);
    }
    for (var j = 0; j < (subrace.asiChoice || 0); j++) {
      values.push(1);
    }
    values.sort(function (a, b) { return b - a; });
    return values;
  }

  function resetPicks(subrace) {
    var values = pattern(subrace);
    state.picks = [];
    for (var i = 0; i < values.length; i++) {
      state.picks.push(null);
    }
    // All six abilities +1 (standard human): no choice to make.
    if (values.length === 6 && values[0] === 1 && values[5] === 1) {
      for (var k = 0; k < abilities.length; k++) {
        state.picks[k] = abilities[k];
      }
    }
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

  function patternText(subrace) {
    var values = pattern(subrace);
    var parts = [];
    for (var i = 0; i < values.length; i++) {
      parts.push("+" + values[i]);
    }
    return parts.join(" / ");
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
          resetPicks(sub);
          renderDetail();
          refreshConfirm();
        });
        subRow.appendChild(btn);
      })(race.subraces[i]);
    }
    detailEl.appendChild(subRow);

    var sub = findSubrace(race, state.subrace);
    if (!sub) return;

    var bonusText = document.createElement("div");
    bonusText.setAttribute("class", "card-sub");
    bonusText.innerHTML = tr("screen.dndclasses.race.bonus_pattern") + " " + patternText(sub);
    detailEl.appendChild(bonusText);

    var needsChoice = pattern(sub).length < 6 || pattern(sub)[0] !== 1;
    if (needsChoice) {
      var hint = document.createElement("div");
      hint.setAttribute("class", "card-sub");
      hint.innerHTML = tr("screen.dndclasses.race.assign_hint");
      detailEl.appendChild(hint);

      var values = pattern(sub);
      for (var slot = 0; slot < values.length; slot++) {
        (function (slotIndex, bonusValue) {
          var row = document.createElement("div");
          row.setAttribute("class", "asi-row");
          var label = document.createElement("span");
          label.setAttribute("class", "rune");
          label.innerHTML = "+" + bonusValue + ":";
          row.appendChild(label);
          for (var a = 0; a < abilities.length; a++) {
            (function (ability) {
              var selected = state.picks[slotIndex] === ability;
              var usedElsewhere = false;
              for (var p = 0; p < state.picks.length; p++) {
                if (p !== slotIndex && state.picks[p] === ability) usedElsewhere = true;
              }
              var btn = document.createElement("button");
              btn.setAttribute("type", "button");
              btn.setAttribute("class", "asi-btn" + (selected ? " active" : ""));
              if (usedElsewhere) btn.setAttribute("disabled", "disabled");
              btn.innerHTML = tr("ability.dndclasses." + ability);
              btn.addEventListener("click", function () {
                state.picks[slotIndex] = ability;
                renderDetail();
                refreshConfirm();
              });
              row.appendChild(btn);
            })(abilities[a]);
          }
          detailEl.appendChild(row);
        })(slot, values[slot]);
      }
    }

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
          resetPicks(race.subraces[0]);
          renderList();
          renderDetail();
          refreshConfirm();
        });
        listEl.appendChild(btn);
      })(races[i]);
    }
  }

  function allPicked() {
    for (var i = 0; i < state.picks.length; i++) {
      if (!state.picks[i]) return false;
    }
    return true;
  }

  function refreshConfirm() {
    var race = findRace(state.race);
    var sub = findSubrace(race, state.subrace);
    if (race && sub && allPicked()) confirmEl.removeAttribute("disabled");
    else confirmEl.setAttribute("disabled", "disabled");
  }

  function bonuses() {
    var result = {};
    var sub = findSubrace(findRace(state.race), state.subrace);
    var values = pattern(sub);
    for (var i = 0; i < values.length; i++) {
      var ability = state.picks[i];
      if (ability) result[ability] = values[i];
    }
    return result;
  }

  confirmEl.addEventListener("click", function () {
    var race = findRace(state.race);
    var sub = findSubrace(race, state.subrace);
    if (!race || !sub) return;
    if (!allPicked()) {
      if (msgEl) msgEl.innerHTML = tr("screen.dndclasses.race.assign_incomplete");
      return;
    }
    window.dndRace = {
      race: race.key,
      subrace: sub.key,
      bonuses: bonuses()
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
