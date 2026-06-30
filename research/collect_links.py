#!/usr/bin/env python3
"""
research/collect_links.py - collect ALL match URLs for a World Cup results page.

OddsHarvester's own collector stops at OddsPortal's first results page (~50
matches), so the tournament openers are missed. OddsPortal renders the results
list virtualized (only the visible rows are in the DOM), so this script scrolls
down in small steps, accumulating every /football/h2h/ link as rows mount, and
writes them to data/wc{season}_links.txt - which you then feed to OddsHarvester
in full mode (all bookmakers).

    python3 research/collect_links.py 2018
    python3 research/collect_links.py 2022

Needs playwright (already installed): python3 -m playwright install chromium
"""
import sys
import time
from playwright.sync_api import sync_playwright

SEASON = sys.argv[1] if len(sys.argv) > 1 else "2018"
RESULTS = f"https://www.oddsportal.com/football/world/world-cup-{SEASON}/results/"
OUT = f"data/wc{SEASON}_links.txt"


def dismiss_cookies(page):
    for sel in ("#onetrust-accept-btn-handler", "button:has-text('I Accept')",
                "button:has-text('Accept')", "button:has-text('AGREE')"):
        try:
            page.click(sel, timeout=2500)
            return
        except Exception:
            pass


def harvest(page, found):
    for h in page.eval_on_selector_all("a[href]", "els => els.map(e => e.href)"):
        u = h.split("#")[0].rstrip("/")
        if "/football/h2h/" in u:
            found.add(u + "/")


def main():
    found = set()
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1400, "height": 1000})
        page.goto(RESULTS, timeout=60000, wait_until="domcontentloaded")
        time.sleep(4)
        dismiss_cookies(page)
        time.sleep(2)

        # small incremental scroll so every virtualized row passes the viewport
        at_bottom = 0
        for _ in range(200):
            harvest(page, found)
            scroll_y, inner_h, total_h = page.evaluate(
                "() => [window.scrollY, window.innerHeight, document.body.scrollHeight]")
            if scroll_y + inner_h >= total_h - 200:
                at_bottom += 1
                if at_bottom >= 5:
                    break
            else:
                at_bottom = 0
            page.mouse.wheel(0, 650)
            time.sleep(0.6)
        harvest(page, found)

        if len(found) < 60:  # diagnostic
            hrefs = page.eval_on_selector_all("a[href]", "els => els.map(e => e.href)")
            foot = sorted({h.split('#')[0] for h in hrefs if '/football/' in h})
            print(f"\n  DIAGNOSTIC: {len(foot)} distinct /football/ links visible now; sample:")
            for h in foot[:25]:
                print("    ", h)
        browser.close()

    links = sorted(found)
    with open(OUT, "w", encoding="utf-8") as f:
        f.write("\n".join(links) + ("\n" if links else ""))
    print(f"\ncollected {len(links)} match links -> {OUT}")
    for l in links[:6]:
        print("   sample:", l)


if __name__ == "__main__":
    main()
