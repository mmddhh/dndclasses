(function () {
  function tr(key) {
    return "<translation>" + key + "</translation>";
  }

  var dataEl = document.getElementById("race-data");
  var listEl = document.getElementById("race-list");
  var detailEl = document.getElementById("race-detail");
  var confirmEl = document.getElementById("race-confirm");
  var msgEl = document.getElementById("race-msg");
  var overlay = document.getElementById("race-overlay");
  if (!dataEl || !listEl || !detailEl || !confirmEl || !overlay) return;

  var data = {};
  try {
    data = JSON.parse(dataEl.value || "{}");
  } catch (err) {
    data = {};
  }
  var races = data.races || [];
  var state = { race: null, subrace: null, asi: [] };

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

  function raceName(key) {
    return tr("race.dndclasses." + key);
  }

  function subraceName(raceKey, key) {
    return tr("race.dndclasses." + raceKey + "." + key);
  }

  function traitName(key) {
    return tr("trait.dndclasses." + key);
  }

  function asiText(subrace) {
    var parts = [];
    if (subrace.asi) {
      var keys = Object.keys(subrace.asi);
      for (var i = 0; i < keys.length; i++) {
        parts.push(tr("ability.dndclasses." + keys[i]) + " +" + subrace.asi[keys[i]]);
      }
    }
    if (subrace.asiChoice > 0) {
      parts.push(tr("screen.dndclasses.race.choose_asi") + " x" + subrace.asiChoice);
    }
    return parts.join("，");
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
    info.innerHTML = tr("screen.dndclasses.race.speed") + " " + race.speed
      + " ft · " + tr("size.dndclasses." + race.size);
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
          state.asi = [];
          renderDetail();
        });
        subRow.appendChild(btn);
      })(race.subraces[i]);
    }
    detailEl.appendChild(subRow);

    var sub = findSubrace(race, state.subrace);
    if (!sub) return;

    var asi = document.createElement("div");
    asi.setAttribute("class", "card-sub");
    asi.innerHTML = asiText(sub);
    detailEl.appendChild(asi);

    if (sub.asiChoice > 0) {
      var asiRow = document.createElement("div");
      asiRow.setAttribute("class", "asi-row");
      var abilities = ["str", "dex", "con", "int", "wis", "cha"];
      for (var j = 0; j < abilities.length; j++) {
        (function (ability) {
          var active = state.asi.indexOf(ability) >= 0;
          var btn = document.createElement("button");
          btn.setAttribute("type", "button");
          btn.setAttribute("class", "asi-btn" + (active ? " active" : ""));
          btn.innerHTML = tr("ability.dndclasses." + ability) + " +1";
          btn.addEventListener("click", function () {
            var index = state.asi.indexOf(ability);
            if (index >= 0) {
              state.asi.splice(index, 1);
            } else if (state.asi.length < sub.asiChoice) {
              state.asi.push(ability);
            }
            renderDetail();
          });
          asiRow.appendChild(btn);
        })(abilities[j]);
      }
      detailEl.appendChild(asiRow);
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
          state.subrace = race.subraces.length === 1 ? race.subraces[0].key : null;
          state.asi = [];
          renderList();
          renderDetail();
          refreshConfirm();
        });
        listEl.appendChild(btn);
      })(races[i]);
    }
  }

  function currentBonuses() {
    var race = findRace(state.race);
    var sub = findSubrace(race, state.subrace);
    var result = {};
    if (!sub) return result;
    if (sub.asi) {
      var keys = Object.keys(sub.asi);
      for (var i = 0; i < keys.length; i++) {
        result[keys[i]] = (result[keys[i]] || 0) + sub.asi[keys[i]];
      }
    }
    for (var j = 0; j < state.asi.length; j++) {
      result[state.asi[j]] = (result[state.asi[j]] || 0) + 1;
    }
    return result;
  }

  function refreshConfirm() {
    var race = findRace(state.race);
    var sub = findSubrace(race, state.subrace);
    var ok = race && sub && state.asi.length === (sub.asiChoice || 0);
    if (ok) confirmEl.removeAttribute("disabled");
    else confirmEl.setAttribute("disabled", "disabled");
  }

  confirmEl.addEventListener("click", function () {
    var race = findRace(state.race);
    var sub = findSubrace(race, state.subrace);
    if (!race || !sub) return;
    if (state.asi.length !== (sub.asiChoice || 0)) {
      if (msgEl) msgEl.innerHTML = tr("screen.dndclasses.race.asi_incomplete");
      return;
    }
    window.dndRace = {
      race: race.key,
      subrace: sub.key,
      asi: state.asi.slice(),
      bonusFor: function (ability) {
        return currentBonuses()[ability] || 0;
      }
    };
    overlay.setAttribute("class", "race-overlay hidden");
    if (typeof window.dndClassRefresh === "function") {
      window.dndClassRefresh();
    }
  });

  renderList();
  renderDetail();
  refreshConfirm();

  // Hide the race step when the same page is reused as the read-only character sheet.
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
