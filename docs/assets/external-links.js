// Every link to another website opens in a new tab: the navigation, buttons and links in the pages alike.
// Runs on every page view, including those loaded by Material's instant navigation.
document$.subscribe(function () {
  document.querySelectorAll("a[href]").forEach(function (link) {
    if (link.hostname && link.hostname !== window.location.hostname) {
      link.setAttribute("target", "_blank");
      link.setAttribute("rel", "noopener");
    }
  });
});
