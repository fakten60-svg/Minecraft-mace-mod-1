/**
 * Injects canonical URL, Open Graph, Twitter card and JSON-LD structured data
 * from window.SITE (website/config/site.js). Included on every page via the
 * shared head; each page sets data-page attributes on <body> so titles stay
 * unique and server-rendered for crawlers.
 */
(function () {
  "use strict";
  if (!window.SITE) return;
  var S = window.SITE;
  var base = document.documentElement;
  var path = base.getAttribute("data-canonical-path") || "/";
  var title = base.getAttribute("data-og-title") || S.clientName + " \u2013 Minecraft Client";
  var desc = base.getAttribute("data-og-description") || S.description;

  function meta(attr, key, content) {
    if (!content) return;
    var el = document.createElement("meta");
    el.setAttribute(attr, key);
    el.setAttribute("content", content);
    document.head.appendChild(el);
  }
  function link(rel, href) {
    var el = document.createElement("link");
    el.setAttribute("rel", rel);
    el.setAttribute("href", href);
    document.head.appendChild(el);
  }

  var pageUrl = S.siteUrl + path;
  var imgUrl = S.siteUrl + "/assets/img/social-preview.png";
  var ogType = path === "/" ? "website" : "website";

  link("canonical", pageUrl);
  meta("property", "og:site_name", S.clientName);
  meta("property", "og:title", title);
  meta("property", "og:description", desc);
  meta("property", "og:type", ogType);
  meta("property", "og:url", pageUrl);
  meta("property", "og:image", imgUrl);
  meta("property", "og:image:width", "1200");
  meta("property", "og:image:height", "630");
  meta("name", "twitter:card", "summary_large_image");
  meta("name", "twitter:title", title);
  meta("name", "twitter:description", desc);
  meta("name", "twitter:image", imgUrl);

  var ld = {
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "WebSite",
        "@id": S.siteUrl + "/#website",
        url: S.siteUrl + "/",
        name: S.clientName,
        description: S.description,
        inLanguage: "en",
      },
      {
        "@type": "SoftwareApplication",
        "@id": S.siteUrl + "/#software",
        name: S.clientName,
        applicationCategory: "GameApplication",
        operatingSystem: "Windows, macOS, Linux (Minecraft Java Edition)",
        softwareVersion: S.version,
        description: S.description,
        author: { "@type": "Person", name: "fakten60", url: S.authorUrl },
        url: S.siteUrl + "/",
        downloadUrl: S.latestJarUrl,
        fileFormat: "application/java-archive",
        offers: { "@type": "Offer", price: "0", priceCurrency: "USD" },
        featureList: [
          "Automated aerial mace sequence",
          "Animated ClickGUI with search and tooltips",
          "Customizable HUD with editor",
          "Config profiles",
          "Cloud config sharing",
          "Friend manager",
          "Theme system",
          "Module keybinds",
        ],
      },
      {
        "@type": "BreadcrumbList",
        "@id": pageUrl + "#breadcrumb",
        itemListElement: [
          {
            "@type": "ListItem",
            position: 1,
            name: S.clientName,
            item: S.siteUrl + "/",
          },
        ],
      },
    ],
  };
  var script = document.createElement("script");
  script.type = "application/ld+json";
  script.textContent = JSON.stringify(ld);
  document.head.appendChild(script);
})();
