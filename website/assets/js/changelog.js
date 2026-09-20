/* Renders window.CHANGELOG into the changelog page. */
(function () {
  "use strict";
  var root = document.getElementById("changelog-root");
  if (!root || !window.CHANGELOG) return;

  function section(title, items) {
    if (!items || !items.length) return "";
    return (
      '<div class="cl-section"><h3>' + title + "</h3><ul>" +
      items.map(function (i) { return "<li>" + i + "</li>"; }).join("") +
      "</ul></div>"
    );
  }

  var html = window.CHANGELOG.map(function (rel, idx) {
    var badge = rel.latest ? '<span class="badge badge-latest">Latest release</span>' : "";
    var h2 =
      rel.version === "Unreleased"
        ? "Unreleased"
        : "Version " + rel.version;
    return (
      (idx > 0 ? '<hr class="cl-divider">' : "") +
      '<article class="changelog-version reveal">' +
      '<header><h2 id="v' + rel.version.replace(/[^a-z0-9.]+/gi, "-") + '">' + h2 + "</h2>" +
      '<time datetime="' + rel.date + '">' + rel.date + "</time>" + badge + "</header>" +
      section("Added", rel.added) +
      section("Changed", rel.changed) +
      section("Fixed", rel.fixed) +
      "</article>"
    );
  }).join("");

  root.innerHTML = html;
  root.querySelectorAll(".reveal").forEach(function (el) { el.classList.add("in"); });
})();
