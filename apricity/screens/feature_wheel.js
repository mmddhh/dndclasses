(function () {
  "use strict";

  var CX = 280;
  var CY = 280;
  var R_IN = 74, R_OUT = 208;          /* must match FeatureWheelOverlay (Java) */
  var MID_R = (R_IN + R_OUT) / 2;
  var SEG = 16;

  function svg(inner) {
    return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" '
      + 'stroke-linecap="round" stroke-linejoin="round">' + inner + '</svg>';
  }

  function bar(x, y, w, h) {
    return '<rect x="' + x + '" y="' + y + '" width="' + w + '" height="' + h
      + '" fill="currentColor" stroke="none"/>';
  }

  /* Class seals (language-neutral). Bars are rects, not lines: AUI's Svg renderer
     skips degenerate (zero-area) strokes such as pure vertical/horizontal lines. */
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

  var wheelEl = document.querySelector(".wheel");
  var input = document.getElementById("wheel-input");
  var sectorLayer = document.getElementById("layer-sectors");
  var dividerLayer = document.getElementById("layer-dividers");
  var labelLayer = document.getElementById("layer-labels");
  var hudSeal = document.getElementById("hud-seal");
  var hudName = document.getElementById("hud-name");
  var hudUses = document.getElementById("hud-uses");
  var hudRest = document.getElementById("hud-rest");
  var noFeatures = document.getElementById("no-features");

  var features = [];
  var strings = {};
  var cls = "";
  var hovered = -1;
  var lastRaw = "";
  var lastIds = "";
  var lastHover = "-999";
  var ready = false;
  var sectorEls = [];
  var booted = false;

  function byId(id) { return document.getElementById(id); }

  function showWheel() {
    if (ready || !wheelEl) return;
    ready = true;
    wheelEl.setAttribute("style", "display:block");
  }

  function setText(el, text) {
    if (!el) return;
    if (el.textContent !== text) el.textContent = text;
  }

  /* ---------- geometry (AUI Math.sin/cos return NaN; use Bhaskara I in degrees) ---------- */

  function dsin(deg) {
    var d = deg % 360;
    if (d < 0) d += 360;
    if (d > 180) {
      var e = 360 - d;
      return -(4 * e * (180 - e)) / (40500 - e * (180 - e));
    }
    return (4 * d * (180 - d)) / (40500 - d * (180 - d));
  }

  function dcos(deg) { return dsin(deg + 90); }

  function fmt(v) { return (Math.round(v * 10) / 10) + "px"; }

  function pt(a, rad) {
    var f = a - 90;
    return fmt(CX + dcos(f) * rad) + " " + fmt(CY + dsin(f) * rad);
  }

  function clipFor(a0, a1, rIn, rOut) {
    var pts = [];
    var k;
    for (k = 0; k <= SEG; k++) { pts.push(pt(a0 + (a1 - a0) * k / SEG, rIn)); }
    for (k = SEG; k >= 0; k--) { pts.push(pt(a0 + (a1 - a0) * k / SEG, rOut)); }
    return "polygon(" + pts.join(", ") + ")";
  }

  function uprightTransform(a, radius) {
    return "translate(-50%,-50%) rotate(" + a + "deg) translateY(-" + radius + "px) rotate(" + (-a) + "deg)";
  }

  /* ---------- data ---------- */

  function pollData() {
    var el = byId("feature-data");
    if (!el) return;
    var raw = el.value;
    if (raw === lastRaw) return;
    lastRaw = raw;
    if (!raw) return;
    try {
      var d = JSON.parse(raw);
      features = d.features || [];
      strings = d.strings || {};
      cls = d.class || "";
      if (noFeatures) noFeatures.textContent = strings.noFeatures || "No features available";

      var ids = "";
      for (var i = 0; i < features.length; i++) ids += features[i].id + "|";
      if (ids !== lastIds) {
        lastIds = ids;
        buildWheel();
      }
      if (hovered >= features.length) hovered = -1;
      updateHud();
      showWheel();
    } catch (e) {
      /* ignore malformed data */
    }
  }

  function buildWheel() {
    if (sectorLayer) sectorLayer.innerHTML = "";
    if (dividerLayer) dividerLayer.innerHTML = "";
    if (labelLayer) labelLayer.innerHTML = "";
    if (hudSeal) hudSeal.innerHTML = SEALS[cls] || "";
    sectorEls = [];
    hovered = -1;

    if (noFeatures) noFeatures.setAttribute("style", features.length === 0 ? "display:block" : "display:none");

    var n = features.length;
    if (n === 0) {
      updateHud();
      return;
    }

    var step = 360 / n;
    for (var i = 0; i < n; i++) {
      var a0 = i * step;
      var a1 = a0 + step;
      var mid = a0 + step / 2;

      var spent = features[i].remaining <= 0 ? " spent" : "";
      var span = a1 - a0;
      var clip = span >= 359.5
        ? ("circle(" + R_OUT + "px at " + CX + "px " + CY + "px)")
        : clipFor(a0, a1, R_IN, R_OUT);
      var sector = document.createElement("div");
      sector.setAttribute("class", "sector" + spent);
      sector.setAttribute("style", "clip-path: " + clip + ";");
      if (sectorLayer) sectorLayer.appendChild(sector);
      sectorEls.push(sector);

      var divider = document.createElement("div");
      divider.setAttribute("class", "divider");
      divider.setAttribute("style", "transform: translate(-50%,-50%) rotate(" + a0 + "deg) translateY(-" + MID_R + "px);");
      if (dividerLayer) dividerLayer.appendChild(divider);

      var label = document.createElement("div");
      label.setAttribute("class", "label");
      label.setAttribute("style", "transform: " + uprightTransform(mid, MID_R) + ";");
      label.textContent = features[i].name;
      if (labelLayer) labelLayer.appendChild(label);
    }

    updateHud();
  }

  /* ---------- hover (index computed by Java) ---------- */

  function pollHover() {
    var el = byId("hover-index");
    if (!el) return;
    var raw = el.value;
    if (!raw || raw === lastHover) return;
    lastHover = raw;
    var idx = parseInt(raw, 10);
    if (!isFinite(idx)) return;
    setHovered(idx);
  }

  function setHovered(index) {
    if (index === hovered) return;
    hovered = index;
    for (var i = 0; i < sectorEls.length; i++) {
      var spent = (i < features.length && features[i].remaining <= 0) ? " spent" : "";
      sectorEls[i].setAttribute("class", (i === hovered ? "sector active" : "sector") + spent);
    }
    updateHud();
  }

  function hoveredFeature() {
    return (hovered >= 0 && hovered < features.length) ? features[hovered] : null;
  }

  function updateHud() {
    var f = hoveredFeature();
    if (!f) {
      setText(hudName, "\u00a0");
      setText(hudUses, "");
      setText(hudRest, "");
      if (hudUses) hudUses.setAttribute("class", "");
      return;
    }
    setText(hudName, f.name);
    var max = (f.max === undefined || f.max === null) ? 1 : f.max;
    setText(hudUses, f.remaining + " / " + max);
    if (hudUses) hudUses.setAttribute("class", f.remaining <= 0 ? "spent" : "");
    setText(hudRest, f.rest === "short" ? (strings.shortRest || "Short Rest")
      : (f.rest === "long" ? (strings.longRest || "Long Rest") : ""));
  }

  /* ---------- boot ---------- */

  function boot() {
    if (booted) return;
    booted = true;
    pollData();
    pollHover();
    setTimeout(showWheel, 600);
    setInterval(function () { pollData(); pollHover(); }, 40);
  }

  document.addEventListener("DOMContentLoaded", boot);
  if (document.body && document.body.addEventListener) {
    document.body.addEventListener("load", boot);
  }
  if (document.readyState === "complete" || document.readyState === "interactive") {
    boot();
  }
})();
