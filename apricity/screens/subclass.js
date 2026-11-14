(function () {
  var dataEl = document.getElementById("choice-data");
  var payloadEl = document.getElementById("choice-payload");
  var listEl = document.getElementById("subclass-list");
  if (!dataEl || !payloadEl || !listEl) return;

  var rendered = false;

  function render() {
    if (rendered) return;
    var data = {};
    try {
      data = JSON.parse(dataEl.value || "{}");
    } catch (err) {
      return;
    }
    var options = data.options || [];
    if (!options.length) return;
    for (var i = 0; i < options.length; i++) {
      (function (option) {
        var button = document.createElement("button");
        button.setAttribute("type", "button");
        button.setAttribute("class", "choice-card");
        button.innerHTML =
          '<span class="card-title">' + option.name + '</span>' +
          '<span class="card-sub">' + option.id + '</span>';
        button.addEventListener("click", function () {
          payloadEl.value = option.id;
        });
        listEl.appendChild(button);
      })(options[i]);
    }
    rendered = true;
  }

  // The screen injects the payload from tick(), which can happen after the page script has run.
  render();
  if (!rendered && typeof setInterval === "function") {
    var timer = setInterval(function () {
      render();
      if (rendered) {
        try {
          clearInterval(timer);
        } catch (ignored) {
        }
      }
    }, 50);
  }
  if (!rendered && typeof setTimeout === "function") {
    setTimeout(function () {
      if (!rendered && !listEl.children.length) {
        listEl.innerHTML =
          '<div class="empty-state"><translation>screen.dndclasses.subclass.none</translation></div>';
      }
    }, 1200);
  }
})();
