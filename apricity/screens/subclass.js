(function () {
  var dataEl = document.getElementById("choice-data");
  var payloadEl = document.getElementById("choice-payload");
  var listEl = document.getElementById("subclass-list");
  var detailEl = document.getElementById("subclass-detail");
  var confirmEl = document.getElementById("subclass-confirm");
  if (!dataEl || !payloadEl || !listEl || !detailEl || !confirmEl) return;

  var started = false;
  var options = [];
  var selected = null;
  var classKey = "";

  function tr(key) {
    return "<translation>" + key + "</translation>";
  }

  function idPath(id) {
    return id && id.indexOf(":") >= 0 ? id.substring(id.indexOf(":") + 1) : (id || "");
  }

  function subclassMeta(index) {
    if (typeof CLASS_FEATURES === "undefined" || !CLASS_FEATURES[classKey]) return null;
    var list = CLASS_FEATURES[classKey].subclasses || [];
    if (index >= 0 && index < list.length) return list[index];
    return null;
  }

  function renderDetail() {
    detailEl.innerHTML = "";
    if (!selected) {
      detailEl.innerHTML = '<div class="empty-state">' +
        tr("screen.dndclasses.subclass.select") + '</div>';
      confirmEl.setAttribute("disabled", "disabled");
      return;
    }

    var path = idPath(selected.id);
    var meta = subclassMeta(selected.index);

    var head = document.createElement("div");
    head.setAttribute("class", "subclass-head");
    head.innerHTML =
      '<div class="subclass-title">' + tr("subclass.dndclasses." + path + ".name") + '</div>' +
      '<div class="subclass-en">' + (meta && meta.en ? meta.en : "") + '</div>';
    detailEl.appendChild(head);

    var features = (meta && meta.features) ? meta.features : [];
    if (!features.length) {
      var empty = document.createElement("div");
      empty.setAttribute("class", "empty-state");
      empty.innerHTML = tr("screen.dndclasses.subclass.no_features");
      detailEl.appendChild(empty);
    } else {
      for (var g = 0; g < features.length; g++) {
        (function (group) {
          var row = document.createElement("div");
          row.setAttribute("class", "subclass-feature-row");
          var lv = document.createElement("div");
          lv.setAttribute("class", "subclass-feature-level");
          lv.innerHTML = "Lv " + group.lv;
          row.appendChild(lv);

          var items = document.createElement("div");
          items.setAttribute("class", "subclass-feature-items");
          for (var i = 0; i < group.items.length; i++) {
            (function (item) {
              var chip = document.createElement("div");
              chip.setAttribute("class", "subclass-feature");
              chip.innerHTML =
                '<span class="feature-cn">' + (item.n || item.e || "") + '</span>' +
                '<span class="feature-en">' + (item.e || "") + '</span>';
              items.appendChild(chip);
            })(group.items[i]);
          }
          row.appendChild(items);
          detailEl.appendChild(row);
        })(features[g]);
      }
    }

    confirmEl.removeAttribute("disabled");
  }

  function select(option) {
    selected = option;
    var cards = listEl.children;
    for (var i = 0; i < cards.length; i++) {
      if (i === option.index) {
        cards[i].setAttribute("class", "subclass-card active");
      } else {
        cards[i].setAttribute("class", "subclass-card");
      }
    }
    renderDetail();
  }

  function render(data) {
    if (started) return;
    if (!data.options || !data.options.length) return;
    started = true;
    classKey = data["class"] || "";
    options = data.options;
    for (var i = 0; i < options.length; i++) {
      (function (option, index) {
        option.index = index;
        var card = document.createElement("button");
        card.setAttribute("type", "button");
        card.setAttribute("class", "subclass-card");
        var path = idPath(option.id);
        card.innerHTML =
          '<span class="card-title">' + tr("subclass.dndclasses." + path + ".name") + '</span>' +
          '<span class="card-sub">' + (option.name || "") + '</span>';
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
