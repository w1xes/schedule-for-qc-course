"""Inspect the actual DOM of admin pages to find correct selectors."""
from playwright.sync_api import sync_playwright
from dotenv import load_dotenv
import os

load_dotenv()
base_url = os.getenv("BASE_URL")
email = os.getenv("ADMIN_EMAIL")
password = os.getenv("ADMIN_PASSWORD")

with sync_playwright() as p:
    browser = p.chromium.launch(headless=False, slow_mo=200)
    ctx = browser.new_context(viewport={"width": 1280, "height": 800})
    page = ctx.new_page()

    # Login
    page.goto(f"{base_url}/login")
    page.wait_for_load_state("networkidle")
    page.locator('input[name="email"]').fill(email)
    page.locator('input[name="password"]').fill(password)
    page.locator('button[type="submit"]').click()
    page.wait_for_url("**/schedule**", timeout=15000)
    print("After login URL:", page.url)

    # Navigate to admin/departments
    page.goto(f"{base_url}/admin/departments")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(2000)
    print("After admin nav URL:", page.url)

    # Check all inputs
    inputs = page.query_selector_all("input")
    print(f"\nInputs found: {len(inputs)}")
    for inp in inputs:
        print(f"  name={inp.get_attribute('name')}, type={inp.get_attribute('type')}, class={inp.get_attribute('class')}")

    # Check buttons
    buttons = page.query_selector_all("button")
    print(f"\nButtons found: {len(buttons)}")
    for btn in buttons:
        print(f"  type={btn.get_attribute('type')}, class={btn.get_attribute('class')}, text={btn.inner_text()[:50]}")

    # Check key container classes
    print("\nKey selectors check:")
    for sel in [
        ".subject-form", ".form-card", ".search-list__panel", "form",
        ".admin-layout", ".MuiCard-root", ".MuiPaper-root",
        "[class*='department']", "[class*='form']", "[class*='card']",
        ".items-container", ".search-list", ".admin"
    ]:
        els = page.query_selector_all(sel)
        if els:
            print(f"  {sel}: {len(els)} elements")
            for e in els[:2]:
                print(f"    class={e.get_attribute('class')}, tag={e.evaluate('el => el.tagName')}")

    # Print a larger HTML section
    print("\n--- MAIN CONTENT HTML (first 4000 chars) ---")
    main = page.query_selector("#root") or page.query_selector("main") or page.query_selector("body")
    if main:
        html = main.inner_html()
        print(html[:4000])

    # Now check groups page
    print("\n\n=== GROUPS PAGE ===")
    page.goto(f"{base_url}/admin/groups")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(2000)
    print("Groups URL:", page.url)

    print("\nKey selectors check on groups:")
    for sel in [
        ".drag-and-drop-card", ".group-card", "[draggable]",
        "[class*='drag']", "[class*='group']", "[class*='card']",
        ".MuiCard-root", ".items-container"
    ]:
        els = page.query_selector_all(sel)
        if els:
            print(f"  {sel}: {len(els)} elements")
            for e in els[:2]:
                cls = e.get_attribute("class") or ""
                print(f"    class={cls[:80]}, draggable={e.get_attribute('draggable')}")

    print("\n--- GROUPS MAIN CONTENT HTML (first 3000 chars) ---")
    main = page.query_selector("#root") or page.query_selector("body")
    if main:
        print(main.inner_html()[:3000])

    browser.close()
