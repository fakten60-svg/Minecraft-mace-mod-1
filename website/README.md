# Gugugaga Client — Website

Standalone static website for the **Gugugaga Client**. No build step, no frameworks, no
external dependencies — plain HTML/CSS/JS that can be hosted anywhere.

> The Minecraft client itself lives in the repository root and is untouched by this folder.

## Pages

| Page | Purpose |
| --- | --- |
| `index.html` | Hero, feature overview, ClickGUI/HUD showcase, download CTA |
| `features.html` | Detailed, honest feature list |
| `download.html` | Latest release + installation steps |
| `documentation.html` | Getting started, ClickGUI, modules, HUD, keybinds, config, cloud, friends, themes |
| `changelog.html` | Releases rendered from `assets/data/changelog.js` |
| `faq.html` | FAQ (with FAQPage structured data) |
| `404.html` | Error page |

## Central configuration

`config/site.js` is the single source of truth for branding, versions and the site URL
(`SITE_URL`). It feeds the canonical URLs, Open Graph/Twitter tags and JSON-LD structured
data injected by `assets/js/head.js`.

**When the final URL goes live**, update in these places (all hold the same origin):

1. `config/site.js` → `siteUrl`
2. `sitemap.xml` → the `<loc>` entries
3. `robots.txt` → the `Sitemap:` line

A global search for `fakten60-svg.github.io/gugugaga-client` finds every occurrence.

### Moving to gugugagaclient.github.io

`gugugagaclient.github.io` is a **user-site URL**; GitHub serves it only from a repo named
exactly `gugugagaclient.github.io` under the owner account. The current Freebuff
credential cannot create repositories, so the one-time setup is manual:

1. Create the public repo **`fakten60-svg/gugugagaclient.github.io`** on GitHub.
2. Copy the contents of this `website/` folder into that repo's default branch root
   (e.g. `git clone https://github.com/fakten60-svg/gugugagaclient.github.io &&
   cp -r website/* gugugagaclient.github.io/ && git -C gugugagaclient.github.io add -A &&
   git -C gugugagaclient.github.io commit -m "Import Gugugaga Client website" &&
   git -C gugugagaclient.github.io push`).
3. Update the three places above to `https://gugugagaclient.github.io` and push.
4. Optional: on the old repo, redirect with a tiny `index.html`:
   `meta http-equiv="refresh" content="0; url=https://gugugagaclient.github.io/"`
   (plus a canonical link), or keep both sites as-is.

Until then the site lives at `https://fakten60-svg.github.io/gugugaga-client/`.

## Content updates

- **New release:** update `config/site.js` (`version`, `latestJarUrl`) and
  `download.html`, then add an entry at the top of `assets/data/changelog.js`
  (source of truth: the repository's `CHANGELOG.md`) and set `latest: true` on it.
- **Screenshots:** `assets/img/client-*.png` are generated from the real client UI
  (`docs/screenshots/*.svg`) via `tools/generate_assets.py` (run from the repo root:
  `python3 website/tools/generate_assets.py`). Replace them with fresh exports whenever
  the GUI changes; the same script regenerates favicons and the social preview.
- **Social preview:** `assets/img/social-preview.png` (1200×630), used for
  Open Graph/Twitter cards.

## SEO

- Unique `<title>` and meta description per page; semantic HTML with a single `<h1>`
- Canonical URLs + Open Graph + Twitter cards + JSON-LD (`WebSite`, `SoftwareApplication`)
- `sitemap.xml` and `robots.txt` (public pages crawlable, `/config` + `/tools` excluded)
- Client name used naturally in nav, hero, headings and footer — no keyword stuffing

## Performance & accessibility

- Zero external requests (system fonts, no trackers, no third-party scripts)
- Lazy-loaded images, `prefers-reduced-motion` respected, GPU-friendly animations only
- Semantic landmarks, skip link, focus states, keyboard-navigable lightbox (Esc/←/→),
  native `<details>` accordions for the FAQ

## Deploy

**Live: https://fakten60-svg.github.io/gugugaga-client/** (planned move: `gugugagaclient.github.io`, see above) — deployed automatically by
`.github/workflows/pages.yml` (GitHub Actions deployment mode) whenever a push to `main`
touches `website/`. Manual runs: `gh workflow run pages.yml`.

Other static hosts work too (Netlify/Vercel with "publish directory" = `website`). Set
the final domain as described under *Central configuration*.
