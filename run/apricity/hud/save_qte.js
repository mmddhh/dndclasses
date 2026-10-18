(function () {
  "use strict";

  var root = document.getElementById("qte-root");
  var box = document.getElementById("qte-box");
  var blocksLayer = document.getElementById("qte-blocks");
  var ringBg = document.getElementById("qte-ring-bg");
  var ringFill = document.getElementById("qte-ring-fill");
  var ringWrap = document.getElementById("qte-ring");
  var button = document.getElementById("qte-button");
  var keyLabel = document.getElementById("qte-key-label");
  var percentEl = document.getElementById("qte-percent");
  var abilityEl = document.getElementById("qte-ability");
  var numbersEl = document.getElementById("qte-numbers");
  var hintEl = document.getElementById("qte-hint");

  var CX = 70, CY = 70, R_IN = 56, R_OUT = 68, SUB = 48;

  var qteId = "";
  var mode = "";
  var boxWidth = 0.3;
  var blocks = [];
  var blockEls = [];
  var results = [];
  var mash = null;
  var now = 0;
  var finished = true;
  var feedbackUntil = 0;
  var lastPress = "0";
  var lastRaw = "";
  var lastKey = "";
  var ringReady = false;
  var tick = null;

  function byId(id) { return document.getElementById(id); }
  function t(k) { return "<translation>" + k + "</translation>"; }
  function pct(v) { return (Math.round(v * 1000) / 10) + "%"; }
  function fmt(v) { return (Math.round(v * 10) / 10) + "px"; }

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

  function pt(a, rad) {
    var f = a - 90;
    return fmt(CX + dcos(f) * rad) + " " + fmt(CY + dsin(f) * rad);
  }

  function clipArc(a0, a1, rIn, rOut) {
    var pts = [];
    var k;
    for (k = 0; k <= SUB; k++) pts.push(pt(a0 + (a1 - a0) * k / SUB, rIn));
    for (k = SUB; k >= 0; k--) pts.push(pt(a0 + (a1 - a0) * k / SUB, rOut));
    return "polygon(" + pts.join(", ") + ")";
  }

  function buildRing() {
    if (ringReady || !ringBg) return;
    ringReady = true;
    ringBg.setAttribute("style", "clip-path:" + clipArc(0, 359.5, R_IN, R_OUT) + ";");
  }

  function writeResults(done) {
    var el = byId("qte-result");
    if (el) el.value = JSON.stringify({ id: qteId, results: results, done: !!done });
  }

  function allResolved() {
    if (results.length === 0) return false;
    for (var i = 0; i < results.length; i++) if (results[i] === null || results[i] === undefined) return false;
    return true;
  }

  function posAt(block) {
    var period = block.period > 0 ? block.period : 1.2;
    var elapsed = now - block.start;
    if (elapsed < 0) return 0;
    var tt = elapsed % (2 * period);
    return tt <= period ? (tt / period) : (2 - tt / period);
  }

  function endTime() {
    var max = 0;
    for (var i = 0; i < blocks.length; i++) {
      var end = blocks[i].start + 2 * (blocks[i].period > 0 ? blocks[i].period : 1.2);
      if (end > max) max = end;
    }
    return max + 0.15;
  }

  function onData(raw) {
    if (!raw) return;
    var d;
    try { d = JSON.parse(raw); } catch (e) { return; }
    var want = d.blocks || [];
    var ab = (want[0] && want[0].ability) ? want[0].ability : "con";

    if (d.id !== qteId) {
      qteId = d.id;
      mode = d.mode || "timing";
      blocks = [];
      blockEls = [];
      results = [];
      now = 0;
      finished = false;
      feedbackUntil = 0;
      lastPress = "0";
      if (blocksLayer) blocksLayer.innerHTML = "";
      root.setAttribute("class", mode + " theme-" + ab);
      buildRing();
    }

    for (var i = 0; i < want.length; i++) {
      if (i < blocks.length) {
        blocks[i].ability = want[i].ability;
        blocks[i].dc = want[i].dc;
        blocks[i].boxWidth = want[i].boxWidth;
        blocks[i].period = want[i].period;
      } else {
        blocks.push({
          ability: want[i].ability, dc: want[i].dc,
          boxWidth: want[i].boxWidth, period: want[i].period,
          start: now, resolved: false, success: false
        });
        results.push(null);
        if (blocksLayer) {
          var el = document.createElement("div");
          el.setAttribute("class", "qte-block");
          blocksLayer.appendChild(el);
          blockEls.push(el);
        }
      }
    }
    if (want.length > 0) boxWidth = want[0].boxWidth || 0.3;
    if (d.mash) {
      mash = d.mash;
      if (mash.progress === undefined) mash.progress = 0;
    }

    if (abilityEl) abilityEl.innerHTML = t("ability.dndclasses." + ab);
    if (numbersEl) numbersEl.textContent = "DC " + ((want[0] && want[0].dc) ? want[0].dc : "-");
    if (hintEl) hintEl.innerHTML = (mode === "mash")
      ? t("screen.dndclasses.qte.mash_hint")
      : t("screen.dndclasses.qte.hint");

    // publish the (all-null) result set immediately so the client never forwards stale data
    writeResults(false);
  }

  function pollKey() {
    var el = byId("qte-key");
    if (!el || !keyLabel) return;
    var raw = el.value;
    if (raw && raw !== lastKey) {
      lastKey = raw;
      keyLabel.textContent = raw;
    }
  }

  function pollPress() {
    var el = byId("qte-press");
    if (!el) return false;
    var raw = el.value;
    if (!raw || raw === lastPress) return false;
    lastPress = raw;
    return true;
  }

  function finishDone() {
    finished = true;
    writeResults(true);
  }

  function press() {
    if (finished) return;
    feedbackUntil = now + 0.12;
    if (mode === "mash") {
      if (mash) {
        mash.progress += mash.perClick;
        if (mash.progress >= mash.required) {
          results[0] = true;
          finishDone();
        }
      }
      return;
    }
    // timing: consume the nearest unresolved block (success if inside the box, else fail)
    var best = -1;
    var bestDist = 1e9;
    for (var i = 0; i < blocks.length; i++) {
      if (results[i] !== null && results[i] !== undefined) continue;
      var dist = Math.abs(posAt(blocks[i]) - 0.5);
      if (dist < bestDist) {
        bestDist = dist;
        best = i;
      }
    }
    if (best >= 0) {
      var success = bestDist <= boxWidth / 2;
      blocks[best].resolved = true;
      blocks[best].success = success;
      results[best] = success;
      writeResults(false);
    }
    if (allResolved()) finishDone();
  }

  function render() {
    var flash = now < feedbackUntil;
    if (mode === "mash") {
      var ratio = mash ? Math.min(0.999, Math.max(0, mash.progress) / mash.required) : 0;
      if (ringFill) {
        if (ratio <= 0) {
          ringFill.setAttribute("style", "display:none;");
        } else {
          ringFill.setAttribute("style", "display:block;clip-path:" + clipArc(0, ratio * 360, R_IN, R_OUT) + ";");
        }
      }
      if (ringWrap) ringWrap.setAttribute("class", flash ? "qte-ring flash" : "qte-ring");
      if (button) button.setAttribute("class", flash ? "qte-button press" : "qte-button");
      if (percentEl) {
        var shown = mash ? Math.round(Math.max(0, mash.progress) * 100 / mash.required) : 0;
        percentEl.textContent = Math.min(100, shown) + "%";
      }
      return;
    }
    for (var i = 0; i < blocks.length; i++) {
      var el = blockEls[i];
      if (!el) continue;
      el.setAttribute("class", "qte-block" + (blocks[i].resolved ? (blocks[i].success ? " done" : " failed") : ""));
      el.setAttribute("style", "left:" + pct(posAt(blocks[i])));
    }
    if (box) {
      box.setAttribute("class", flash ? "qte-box hit" : "qte-box");
      box.setAttribute("style", "left:" + pct(0.5 - boxWidth / 2) + ";width:" + pct(boxWidth));
    }
  }

  function step() {
    var rawEl = byId("qte-data");
    if (rawEl && rawEl.value !== lastRaw) {
      lastRaw = rawEl.value;
      onData(lastRaw);
    }
    pollKey();
    if (finished || !mode) return;
    now += 0.016;
    if (pollPress()) press();
    if (finished) return;
    if (mode === "mash") {
      if (mash) {
        mash.progress -= (mash.decay || 0) * 0.016;
        if (mash.progress < 0) mash.progress = 0;
        if (now >= mash.timeLimit) {
          results[0] = false;
          finishDone();
          return;
        }
      }
    } else if (now >= endTime()) {
      for (var i = 0; i < results.length; i++) {
        if (results[i] === null || results[i] === undefined) {
          results[i] = false;
          blocks[i].resolved = true;
          blocks[i].success = false;
        }
      }
      finishDone();
      return;
    }
    render();
  }

  function boot() {
    if (tick) return;
    buildRing();
    if (typeof setInterval === "function") tick = setInterval(step, 16);
  }

  document.addEventListener("DOMContentLoaded", boot);
  if (document.readyState === "complete" || document.readyState === "interactive") boot();
})();
