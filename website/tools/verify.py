#!/usr/bin/env python3
"""Verifies the website: local links, referenced assets, HTTP responses.

Run from anywhere; always operates on the website/ directory next to this file.
"""
import os
import re
import sys
import threading
import urllib.request
from http.server import HTTPServer, SimpleHTTPRequestHandler

SITE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.chdir(SITE)

errors = []

# 1. Every local href/src target must exist.
for page in [f for f in os.listdir(".") if f.endswith(".html")]:
    html = open(page, encoding="utf-8").read()
    for attr, target in re.findall(r'(href|src)="([^"]+)"', html):
        if target.startswith(("http", "mailto:")):
            continue
        clean = target.split("#")[0]
        if not clean:
            continue
        if not os.path.exists(clean):
            errors.append(f"{page}: broken reference -> {target}")

# 2. Anchor targets referenced within pages exist.
for page in [f for f in os.listdir(".") if f.endswith(".html")]:
    html = open(page, encoding="utf-8").read()
    ids = set(re.findall(r'id="([^"]+)"', html))
    for target in re.findall(r'href="#([^"]+)"', html):
        if target not in ids:
            errors.append(f"{page}: missing anchor -> #{target}")

# 3. Serve and fetch every page.
httpd = HTTPServer(("127.0.0.1", 8123), SimpleHTTPRequestHandler)
threading.Thread(target=httpd.serve_forever, daemon=True).start()

pages = ["index.html", "features.html", "download.html", "documentation.html",
         "changelog.html", "faq.html", "404.html", "robots.txt", "sitemap.xml",
         "assets/css/site.css", "assets/js/site.js", "assets/js/changelog.js",
         "assets/js/head.js", "config/site.js", "assets/data/changelog.js",
         "assets/img/social-preview.png", "assets/img/client-clickgui.png"]
for p in pages:
    try:
        with urllib.request.urlopen(f"http://127.0.0.1:8123/{p}") as r:
            if r.status != 200:
                errors.append(f"HTTP {r.status}: {p}")
    except Exception as exc:  # noqa: BLE001
        errors.append(f"fetch failed: {p} ({exc})")
httpd.shutdown()

# 4. Sitemap URLs must match real files.
sm = open("sitemap.xml", encoding="utf-8").read()
for loc in re.findall(r"<loc>([^<]+)</loc>", sm):
    path = loc.replace("https://fakten60-svg.github.io/gugugaga-client", "").lstrip("/")
    if path and not os.path.exists(path):
        errors.append(f"sitemap references missing file: {loc}")

if errors:
    print("FAILED:")
    for e in errors:
        print("  -", e)
    sys.exit(1)
print("All checks passed: links, anchors, assets, HTTP 200s, sitemap.")
