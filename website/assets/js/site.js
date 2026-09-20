/* Gugugaga Client website — shared progressive enhancements (no dependencies). */
(function () {
  "use strict";

  /* ---------- Mobile navigation ---------- */
  var toggle = document.querySelector(".nav-toggle");
  var links = document.querySelector(".nav-links");
  if (toggle && links) {
    toggle.addEventListener("click", function () {
      var open = links.classList.toggle("open");
      toggle.setAttribute("aria-expanded", open ? "true" : "false");
    });
    links.addEventListener("click", function (e) {
      if (e.target.tagName === "A") {
        links.classList.remove("open");
        toggle.setAttribute("aria-expanded", "false");
      }
    });
  }

  /* ---------- Scroll reveal (skipped for reduced motion / no JS) ---------- */
  var reveals = document.querySelectorAll(".reveal");
  var reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (reveals.length && "IntersectionObserver" in window && !reduced) {
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) {
          entry.target.classList.add("in");
          io.unobserve(entry.target);
        }
      });
    }, { threshold: 0.12 });
    reveals.forEach(function (el) { io.observe(el); });
  } else {
    reveals.forEach(function (el) { el.classList.add("in"); });
  }

  /* ---------- Gallery lightbox ---------- */
  var lightbox = document.getElementById("lightbox");
  if (lightbox) {
    var lbImg = lightbox.querySelector("img");
    var lbCap = lightbox.querySelector("figcaption");
    var items = Array.prototype.slice.call(document.querySelectorAll(".gallery-item"));
    var current = 0;

    function show(index) {
      current = (index + items.length) % items.length;
      var item = items[current];
      var img = item.querySelector("img");
      lbImg.src = item.getAttribute("data-full") || img.src;
      lbImg.alt = img.alt;
      lbCap.textContent = img.alt;
    }
    function openLb(index) {
      show(index);
      lightbox.classList.add("open");
      document.body.style.overflow = "hidden";
      lightbox.querySelector(".lightbox-close").focus();
    }
    function closeLb() {
      lightbox.classList.remove("open");
      document.body.style.overflow = "";
      items[current].focus();
    }

    items.forEach(function (item, i) {
      item.addEventListener("click", function () { openLb(i); });
    });
    lightbox.querySelector(".lightbox-close").addEventListener("click", closeLb);
    lightbox.querySelector(".lightbox-prev").addEventListener("click", function () { show(current - 1); });
    lightbox.querySelector(".lightbox-next").addEventListener("click", function () { show(current + 1); });
    lightbox.addEventListener("click", function (e) {
      if (e.target === lightbox) closeLb();
    });
    document.addEventListener("keydown", function (e) {
      if (!lightbox.classList.contains("open")) return;
      if (e.key === "Escape") closeLb();
      else if (e.key === "ArrowLeft") show(current - 1);
      else if (e.key === "ArrowRight") show(current + 1);
    });
  }

  /* ---------- FAQ: close other open items ---------- */
  var faqs = document.querySelectorAll(".faq-item");
  faqs.forEach(function (item) {
    item.addEventListener("toggle", function () {
      if (!item.open) return;
      faqs.forEach(function (other) {
        if (other !== item) other.open = false;
      });
    });
  });
})();
