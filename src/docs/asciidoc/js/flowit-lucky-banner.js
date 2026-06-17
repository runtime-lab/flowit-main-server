(function () {
  var SHOW_PROBABILITY = 0.5;
  var SHOWN_DATE_STORAGE_KEY = "flowitLuckyBannerShownDate";
  var DAY_IMAGE_SRC = "image/day.png";
  var NIGHT_IMAGE_SRC = "image/night.png";
  var DAY_START_MINUTES = (5 * 60) + 30;
  var NIGHT_START_MINUTES = 19 * 60;
  var FADE_IN_MS = 2500;
  var VISIBLE_MS = 2000;
  var FADE_OUT_MS = 3500;
  var OPACITY_EASING = "linear";
  var TRANSFORM_EASING = "cubic-bezier(0.2, 0, 0, 1)";

  function shouldShow() {
    return Math.random() < SHOW_PROBABILITY;
  }

  function getTodayKey(now) {
    var month = String(now.getMonth() + 1).padStart(2, "0");
    var day = String(now.getDate()).padStart(2, "0");

    return now.getFullYear() + "-" + month + "-" + day;
  }

  function hasShownToday(todayKey) {
    try {
      return window.localStorage.getItem(SHOWN_DATE_STORAGE_KEY) === todayKey;
    }
    catch (error) {
      return false;
    }
  }

  function markShownToday(todayKey) {
    try {
      window.localStorage.setItem(SHOWN_DATE_STORAGE_KEY, todayKey);
    }
    catch (error) {
      return;
    }
  }

  function getBannerImageSrc(now) {
    var minutes = (now.getHours() * 60) + now.getMinutes();

    if (minutes >= DAY_START_MINUTES && minutes < NIGHT_START_MINUTES) {
      return DAY_IMAGE_SRC;
    }

    return NIGHT_IMAGE_SRC;
  }

  function removeOverlay(overlay) {
    if (overlay && overlay.parentNode) {
      overlay.parentNode.removeChild(overlay);
    }
  }

  function setOverlayTransition(overlay, durationMs) {
    overlay.style.transition = "opacity " + durationMs + "ms " + OPACITY_EASING + ", transform "
      + durationMs + "ms " + TRANSFORM_EASING;
  }

  function onceOpacityTransitionEnds(element, durationMs, callback) {
    var done = false;
    var timeoutId = null;

    function finish() {
      if (done) {
        return;
      }

      done = true;
      element.removeEventListener("transitionend", onTransitionEnd);
      if (timeoutId !== null) {
        window.clearTimeout(timeoutId);
      }
      if (callback) {
        callback();
      }
    }

    function onTransitionEnd(event) {
      if (event.target === element && event.propertyName === "opacity") {
        finish();
      }
    }

    element.addEventListener("transitionend", onTransitionEnd);
    timeoutId = window.setTimeout(finish, durationMs + 180);
  }

  function fadeIn(overlay, durationMs, callback) {
    setOverlayTransition(overlay, durationMs);
    onceOpacityTransitionEnds(overlay, durationMs, callback);

    window.requestAnimationFrame(function () {
      overlay.style.opacity = "1";
      overlay.style.transform = "translateY(0) scale(1)";
    });
  }

  function fadeOut(overlay, durationMs, callback) {
    setOverlayTransition(overlay, durationMs);
    onceOpacityTransitionEnds(overlay, durationMs, callback);

    window.requestAnimationFrame(function () {
      overlay.style.opacity = "0";
      overlay.style.transform = "translateY(-8px) scale(0.99)";
    });
  }

  function showOverlay() {
    var now = new Date();
    var todayKey = getTodayKey(now);

    if (hasShownToday(todayKey) || !shouldShow()) {
      return;
    }

    var overlay = document.createElement("div");
    var image = document.createElement("img");

    overlay.className = "lucky-banner-overlay";
    overlay.setAttribute("aria-hidden", "true");
    overlay.style.opacity = "0";
    overlay.style.transform = "translateY(10px) scale(0.985)";

    image.src = getBannerImageSrc(now);
    image.alt = "";
    image.decoding = "async";

    image.addEventListener("load", function () {
      overlay.appendChild(image);
      document.body.appendChild(overlay);
      markShownToday(todayKey);

      fadeIn(overlay, FADE_IN_MS, function () {
        window.setTimeout(function () {
          fadeOut(overlay, FADE_OUT_MS, function () {
            removeOverlay(overlay);
          });
        }, VISIBLE_MS);
      });
    }, { once: true });

    image.addEventListener("error", function () {
      removeOverlay(overlay);
    }, { once: true });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", showOverlay, { once: true });
  }
  else {
    showOverlay();
  }
}());
