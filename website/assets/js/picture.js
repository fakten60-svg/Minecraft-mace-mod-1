/* Upgrades <img data-webp="..."> to WebP when the browser supports it.
 * Without JS (or without WebP) the PNG in src= stays as the fallback. */
(function () {
  "use strict";
  function supportsWebp(cb) {
    var img = new Image();
    img.onload = function () { cb(img.width === 2 && img.height === 2); };
    img.onerror = function () { cb(false); };
    img.src = "data:image/webp;base64,UklGRhoAAABXRUJQVlA4TA0AAAAvAAAAEAcQERGIiP4HAA==";
  }
  supportsWebp(function (ok) {
    if (!ok) return;
    document.querySelectorAll("img[data-webp]").forEach(function (img) {
      img.src = img.getAttribute("data-webp");
    });
  });
})();
