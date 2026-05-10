"""Inspect the actual DOM of admin pages with JWT token set properly."""
from playwright.sync_api import sync_playwright
from dotenv import load_dotenv
import os

load_dotenv()
base_url = os.getenv("BASE_URL")
email = os.getenv("ADMIN_EMAIL")
password = os.getenv("ADMIN_PASSWORD")

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
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

    # Extract JWT token from localStorage
    token = page.evaluate("localStorage.getItem('token')")
    print("Token:", token[:50] if token else "None")

    # Set token as Authorization header
    if token:
        ctx.set_extra_http_headers({"Authorization": token})

    # Navigate to admin/departments
    page.goto(f"{base_url}/admin/departments")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(2000)
    print("After admin nav URL:", page.url)

    # Check all inputs
    inputs = page.query_selector_all("input")
    print(f"\nInputs found: {len(inputs)}")
    for inp in inputs:
        print(f"  name={inp.get_attribute('name')}, type={inp.get_attribute('type')}, placeholder={inp.get_attribute('placeholder')}")

    # Check buttons
    buttons = page.query_selector_all("button")
    print(f"\nButtons found: {len(buttons)}")
    for btn in buttons:
        print(f"  type={btn.get_attribute('type')}, class={btn.get_attribute('class')}, text={btn.inner_text()[:60]!r}")

    # Check key container classes
    print("\nKey selectors check:")
    for sel in [
        ".subject-form", ".form-card", ".search-list__panel", "form",
        ".admin-layout", ".MuiCard-root", ".MuiPaper-root",
        "[class*='department']", "[class*='form']", "[class*='card']",
        ".items-container", ".search-list", ".main-container",
        "[class*='subject']", "[class*='list']",
    ]:
        els = page.query_selector_all(sel)
        if els:
            print(f"  {sel}: {len(els)} elements")
            for e in els[:2]:
                cls = e.get_attribute("class") or ""
                tag = e.evaluate("el => el.tagName")
                print(f"    <{tag}> class={cls[:80]}")

    # Print main content HTML
    print("\n--- PAGE HTML (first 5000 chars) ---")
    root = page.query_selector("#root")
    if root:
        print(root.inner_html()[:5000])
    else:
        print(page.content()[:5000])

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
        ".MuiCard-root", ".items-container", ".search-list__panel"
    ]:
        els = page.query_selector_all(sel)
        if els:
            print(f"  {sel}: {len(els)} elements")
            for e in els[:3]:
                cls = e.get_attribute("class") or ""
                print(f"    class={cls[:80]}, draggable={e.get_attribute('draggable')}")

    print("\n--- GROUPS HTML (first 4000 chars) ---")
    root = page.query_selector("#root")
    if root:
        print(root.inner_html()[:4000])

    browser.close()
