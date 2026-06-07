(function () {
  var DETAILS_ANIMATION_MS = 220;
  var DETAILS_ANIMATION_EASING = "cubic-bezier(0.2, 0, 0, 1)";

  function scheduleNavigationScroll() {
    if ("requestAnimationFrame" in window) {
      window.requestAnimationFrame(centerCurrentNavigationItem);
      return;
    }

    window.setTimeout(centerCurrentNavigationItem, 0);
  }

  function centerCurrentNavigationItem() {
    var nav = document.querySelector(".docs-global-nav");
    if (!nav) {
      return;
    }

    var current = nav.querySelector(".is-current");
    if (!current || nav.scrollHeight <= nav.clientHeight) {
      return;
    }

    var navRect = nav.getBoundingClientRect();
    var currentRect = current.getBoundingClientRect();
    var offset = currentRect.top - navRect.top - (nav.clientHeight / 2) + (currentRect.height / 2);

    nav.scrollTop += offset;
  }

  function directDetailsContent(details) {
    for (var i = 0; i < details.children.length; i += 1) {
      if (details.children[i].classList.contains("content")) {
        return details.children[i];
      }
    }

    return null;
  }

  function cancelRunningDetailsAnimation(details) {
    if (details._flowitDetailsAnimationCleanup) {
      details._flowitDetailsAnimationCleanup();
      details._flowitDetailsAnimationCleanup = null;
    }
  }

  function resetDetailsAnimationState(details, content) {
    details.classList.remove("is-animating");
    details.removeAttribute("data-animating");
    content.style.height = "";
    content.style.opacity = "";
    content.style.overflow = "";
    content.style.transition = "";
    content.style.willChange = "";
    details._flowitDetailsAnimationCleanup = null;
  }

  function finishDetailsAnimationAfterTransition(details, content, afterFinish) {
    var done = false;
    var timeoutId = null;

    function cleanupListener() {
      content.removeEventListener("transitionend", onTransitionEnd);
      if (timeoutId !== null) {
        window.clearTimeout(timeoutId);
      }
    }

    function finish() {
      if (done) {
        return;
      }

      done = true;
      cleanupListener();
      if (afterFinish) {
        afterFinish();
      }

      resetDetailsAnimationState(details, content);
    }

    function onTransitionEnd(event) {
      if (event.target === content && event.propertyName === "height") {
        finish();
      }
    }

    details._flowitDetailsAnimationCleanup = cleanupListener;
    content.addEventListener("transitionend", onTransitionEnd);
    timeoutId = window.setTimeout(finish, DETAILS_ANIMATION_MS + 120);
  }

  function animateDetailsOpen(details, content) {
    cancelRunningDetailsAnimation(details);

    details.open = true;
    details.classList.add("is-animating");
    content.style.height = "0px";
    content.style.opacity = "0";
    content.style.overflow = "hidden";
    content.style.transition = "none";
    content.style.willChange = "height, opacity";

    content.offsetHeight;
    var targetHeight = content.scrollHeight;
    content.style.transition = "height " + DETAILS_ANIMATION_MS + "ms " + DETAILS_ANIMATION_EASING + ", opacity 160ms ease";

    window.requestAnimationFrame(function () {
      content.style.height = targetHeight + "px";
      content.style.opacity = "1";
    });
    finishDetailsAnimationAfterTransition(details, content);
  }

  function animateDetailsClose(details, content) {
    cancelRunningDetailsAnimation(details);

    details.classList.add("is-animating");
    content.style.height = content.scrollHeight + "px";
    content.style.opacity = "1";
    content.style.overflow = "hidden";
    content.style.transition = "none";
    content.style.willChange = "height, opacity";

    content.offsetHeight;
    content.style.transition = "height " + DETAILS_ANIMATION_MS + "ms " + DETAILS_ANIMATION_EASING + ", opacity 160ms ease";

    window.requestAnimationFrame(function () {
      content.style.height = "0px";
      content.style.opacity = "0";
    });
    finishDetailsAnimationAfterTransition(details, content, function () {
      details.open = false;
    });
  }

  function setupDetailsAnimation() {
    var detailsBlocks = document.querySelectorAll("#content details");
    for (var i = 0; i < detailsBlocks.length; i += 1) {
      var details = detailsBlocks[i];
      var summary = details.querySelector("summary");
      var content = directDetailsContent(details);
      if (!summary || !content || details.dataset.detailsAnimationReady === "true") {
        continue;
      }

      details.dataset.detailsAnimationReady = "true";
      summary.addEventListener("click", function (event) {
        event.preventDefault();
        var currentDetails = this.parentElement;
        var currentContent = directDetailsContent(currentDetails);
        if (!currentContent) {
          return;
        }

        if (currentDetails.dataset.animating === "true") {
          return;
        }

        currentDetails.dataset.animating = "true";
        if (currentDetails.open) {
          animateDetailsClose(currentDetails, currentContent);
        }
        else {
          animateDetailsOpen(currentDetails, currentContent);
        }
      });
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", function () {
      scheduleNavigationScroll();
      setupDetailsAnimation();
    });
  }
  else {
    scheduleNavigationScroll();
    setupDetailsAnimation();
  }

  window.addEventListener("load", scheduleNavigationScroll);
  window.addEventListener("load", setupDetailsAnimation);
  window.addEventListener("resize", scheduleNavigationScroll);
}());
