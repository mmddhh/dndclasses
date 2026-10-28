(function () {
  "use strict";

  var dataEl = document.getElementById("stamina-data");
  var fillEl = document.getElementById("stamina-fill");
  var valueEl = document.getElementById("stamina-value");

  var lastRaw = "";

  function update(raw) {
    var data;
    try { data = JSON.parse(raw); } catch (e) { return; }
    var stamina = Number(data.stamina) || 0;
    var max = Number(data.max) || 100;
    var ratio = Math.max(0, Math.min(1, stamina / max));

    if (fillEl) {
      fillEl.setAttribute("style", "width:" + (ratio * 100).toFixed(1) + "%;");
      fillEl.setAttribute("class", ratio <= 0.3 ? "stamina-fill low" : "stamina-fill");
    }
    if (valueEl) {
      valueEl.textContent = Math.round(stamina) + " / " + Math.round(max);
    }
  }

  function step() {
    if (!dataEl) return;
    var raw = dataEl.value;
    if (raw && raw !== lastRaw) {
      lastRaw = raw;
      update(raw);
    }
  }

  function boot() {
    if (typeof setInterval === "function") setInterval(step, 50);
  }

  document.addEventListener("DOMContentLoaded", boot);
  if (document.readyState === "complete" || document.readyState === "interactive") boot();
})();
