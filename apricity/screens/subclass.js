(function () {
  var dataEl = document.getElementById("choice-data");
  var payloadEl = document.getElementById("choice-payload");
  var listEl = document.getElementById("subclass-list");
  var detailEl = document.getElementById("subclass-detail");
  var summaryEl = document.getElementById("subclass-summary");
  var footerEl = document.getElementById("footer-summary");
  var confirmEl = document.getElementById("subclass-confirm");
  if (!dataEl || !payloadEl || !listEl || !detailEl || !confirmEl) return;

  var started = false;
  var options = [];
  var selected = null;
  var classKey = "";
  var playerLevel = 1;

  function tr(key) {
    return "<translation>" + key + "</translation>";
  }

  function idPath(id) {
    return id && id.indexOf(":") >= 0 ? id.substring(id.indexOf(":") + 1) : (id || "");
  }

  function slugify(text) {
    return String(text || "")
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, "_")
      .replace(/^_+|_+$/g, "");
  }

  function subclassMeta(index) {
    if (typeof CLASS_FEATURES === "undefined" || !CLASS_FEATURES[classKey]) return null;
    var list = CLASS_FEATURES[classKey].subclasses || [];
    if (index >= 0 && index < list.length) return list[index];
    return null;
  }

  function featureBlock(item) {
    var box = document.createElement("div");
    box.setAttribute("class", "feature");
    var key = "feature.dndclasses." + slugify(item.e || item.n);
    var title = document.createElement("div");
    title.setAttribute("class", "feature-title");
    var name = document.createElement("span");
    name.setAttribute("class", "feature-name");
    name.innerHTML = tr(key) + tr(key + ".subtitle");
    title.appendChild(name);
    box.appendChild(title);
    var text = document.createElement("div");
    text.setAttribute("class", "feature-text");
    if (item.t) {
      text.textContent = item.t;
    } else {
      text.innerHTML = tr("feature.dndclasses.desc_placeholder");
    }
    box.appendChild(text);
    return box;
  }

  function levelRow(level, items) {
    var row = document.createElement("div");
    row.setAttribute("class", "level-row");
    var head = document.createElement("div");
    head.setAttribute("class", "level-head");
    var badge = document.createElement("span");
    badge.setAttribute("class", "level-badge");
    badge.innerHTML = "Lv " + level;
    head.appendChild(badge);
    row.appendChild(head);

    var list = document.createElement("div");
    list.setAttribute("class", "level-items");
    for (var i = 0; i < items.length; i++) {
      list.appendChild(featureBlock(items[i]));
    }
    row.appendChild(list);
    return row;
  }

  function renderSummary() {
    if (!summaryEl) return;
    summaryEl.innerHTML = "";
    var cls = document.createElement("div");
    cls.setAttribute("class", "summary-row");
    cls.innerHTML = tr("screen.dndclasses.subclass.class_label") + " " +
      tr("class.dndclasses." + classKey);
    summaryEl.appendChild(cls);

    var lv = document.createElement("div");
    lv.setAttribute("class", "summary-row");
    lv.innerHTML = tr("screen.dndclasses.subclass.level") + " " + playerLevel;
    summaryEl.appendChild(lv);

    var pick = document.createElement("div");
    pick.setAttribute("class", "summary-row");
    if (selected) {
      pick.innerHTML = tr("screen.dndclasses.subclass.selected") + " " +
        tr("subclass.dndclasses." + idPath(selected.id) + ".name");
    } else {
      pick.innerHTML = tr("screen.dndclasses.subclass.select");
    }
    summaryEl.appendChild(pick);

    if (footerEl) {
      footerEl.innerHTML = selected
        ? tr("subclass.dndclasses." + idPath(selected.id) + ".name")
        : tr("screen.dndclasses.subclass.select");
    }
  }

  function renderDetail() {
    detailEl.innerHTML = "";
    if (!selected) {
      detailEl.innerHTML = '<div class="empty-state">' +
        tr("screen.dndclasses.subclass.select") + '</div>';
      confirmEl.setAttribute("disabled", "disabled");
      renderSummary();
      return;
    }

    var path = idPath(selected.id);
    var meta = subclassMeta(selected.index);

    var hero = document.createElement("div");
    hero.setAttribute("class", "detail-hero");
    var ht = document.createElement("div");
    var name = document.createElement("div");
    name.setAttribute("class", "detail-name");
    name.innerHTML = tr("subclass.dndclasses." + path + ".name");
    ht.appendChild(name);
    if (meta && meta.en) {
      var en = document.createElement("div");
      en.setAttribute("class", "detail-en");
      en.textContent = meta.en;
      ht.appendChild(en);
    }
    hero.appendChild(ht);
    detailEl.appendChild(hero);

    var features = (meta && meta.features) ? meta.features : [];
    if (!features.length) {
      var empty = document.createElement("div");
      empty.setAttribute("class", "empty-state");
      empty.innerHTML = tr("screen.dndclasses.subclass.no_features");
      detailEl.appendChild(empty);
    } else {
      for (var g = 0; g < features.length; g++) {
        detailEl.appendChild(levelRow(features[g].lv, features[g].items || []));
      }
    }

    confirmEl.removeAttribute("disabled");
    renderSummary();
  }

  function select(option) {
    selected = option;
    for (var i = 0; i < listEl.children.length; i++) {
      if (i === option.index) {
        listEl.children[i].setAttribute("class", "race-item active");
      } else {
        listEl.children[i].setAttribute("class", "race-item");
      }
    }
    renderDetail();
  }

  function render(data) {
    if (started) return;
    if (!data.options || !data.options.length) return;
    started = true;
    classKey = data["class"] || "";
    playerLevel = data.level || 1;
    options = data.options;

    for (var i = 0; i < options.length; i++) {
      (function (option, index) {
        option.index = index;
        var card = document.createElement("button");
        card.setAttribute("type", "button");
        card.setAttribute("class", "race-item");
        card.innerHTML = tr("subclass.dndclasses." + idPath(option.id) + ".name");
        card.addEventListener("click", function () {
          select(option);
        });
        listEl.appendChild(card);
      })(options[i], i);
    }
    select(options[0]);
  }

  function tryRender() {
    if (started) return;
    var raw = dataEl.value || "";
    if (!raw || raw === "{}") return;
    var data;
    try {
      data = JSON.parse(raw);
    } catch (err) {
      return;
    }
    render(data);
  }

  confirmEl.addEventListener("click", function () {
    if (!selected) return;
    payloadEl.value = selected.id;
  });

  tryRender();
  if (!started && typeof setInterval === "function") {
    var timer = setInterval(function () {
      tryRender();
      if (started) {
        try {
          clearInterval(timer);
        } catch (ignored) {
        }
      }
    }, 50);
  }
  if (!started && typeof setTimeout === "function") {
    setTimeout(function () {
      if (!started && !listEl.children.length) {
        listEl.innerHTML = '<div class="empty-state">' +
          tr("screen.dndclasses.subclass.none") + '</div>';
      }
    }, 1200);
  }
})();
