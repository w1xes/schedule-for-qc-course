"""
conftest.py — shared pytest fixtures for all E2E tests.

Fixtures:
  base_url        – frontend base URL (from .env)
  api_url         – backend REST API base URL
  admin_email     – manager login e-mail
  admin_password  – manager login password
  db              – DBHelper instance
  browser_context – Playwright browser context (session-scoped)
  page            – Playwright page (function-scoped, fresh for each test)
  authenticated_page – page already logged in as admin (function-scoped)
  login_page      – LoginPage PO bound to `page`
  department_page – DepartmentPage PO, navigated and logged in
  group_page      – GroupPage PO, navigated and logged in
"""
import os

import pytest
from dotenv import load_dotenv
from playwright.sync_api import sync_playwright, Page

# Load .env from the e2e directory
load_dotenv(os.path.join(os.path.dirname(__file__), ".env"))

from pages.login_page import LoginPage
from pages.department_page import DepartmentPage
from pages.group_page import GroupPage
from helpers.db_helper import make_db_helper, DBHelper


# ---------------------------------------------------------------------------
# Configuration fixtures
# ---------------------------------------------------------------------------

@pytest.fixture(scope="session")
def base_url() -> str:
    return os.getenv("BASE_URL", "http://localhost:3000")


@pytest.fixture(scope="session")
def api_url() -> str:
    return os.getenv("API_URL", "http://localhost:8081")


@pytest.fixture(scope="session")
def admin_email() -> str:
    return os.getenv("ADMIN_EMAIL", "manager@gmail.com")


@pytest.fixture(scope="session")
def admin_password() -> str:
    return os.getenv("ADMIN_PASSWORD", "Qwerty!123")


# ---------------------------------------------------------------------------
# Database fixture
# ---------------------------------------------------------------------------

@pytest.fixture(scope="session")
def db() -> DBHelper:
    return make_db_helper()


# ---------------------------------------------------------------------------
# Playwright fixtures
# ---------------------------------------------------------------------------

@pytest.fixture(scope="session")
def playwright_instance():
    with sync_playwright() as pw:
        yield pw


@pytest.fixture(scope="session")
def browser(playwright_instance):
    headless = os.getenv("HEADLESS", "true").lower() == "true"
    slow_mo = int(os.getenv("SLOW_MO", "0"))
    br = playwright_instance.chromium.launch(headless=headless, slow_mo=slow_mo)
    yield br
    br.close()


@pytest.fixture(scope="function")
def context(browser):
    ctx = browser.new_context(
        viewport={"width": 1280, "height": 800},
        locale="en-US",
    )
    yield ctx
    ctx.close()


@pytest.fixture(scope="function")
def page(context) -> Page:
    pg = context.new_page()
    yield pg
    pg.close()


# ---------------------------------------------------------------------------
# Authenticated page fixture
# ---------------------------------------------------------------------------

@pytest.fixture(scope="function")
def authenticated_page(page, base_url, admin_email, admin_password) -> Page:
    """
    Return a page that has already completed the admin login flow.

    Spring Security blocks direct GET requests to /admin/** unless the
    JWT token is present in the Authorization header.  The token lives
    only in localStorage, so we extract it after login and inject it
    as an extra HTTP header on the browser context before doing any
    goto() to /admin/* URLs.
    """
    lp = LoginPage(page, base_url)
    lp.open()
    lp.login(admin_email, admin_password)
    # Wait for the post-login redirect (lands on /schedule)
    lp.wait_for_redirect_after_login("/schedule")

    # Grab the JWT token the SPA stored in localStorage after successful auth
    token = page.evaluate("localStorage.getItem('token')")
    if token:
        # Attach the token to every subsequent HTTP request made by this context
        page.context.set_extra_http_headers({"Authorization": token})

    # Now the server will accept a direct request to any /admin/** route
    page.goto(f"{base_url}/admin/departments")
    page.wait_for_url("**/admin/**", timeout=15_000)
    return page


# ---------------------------------------------------------------------------
# Page Object fixtures
# ---------------------------------------------------------------------------

@pytest.fixture(scope="function")
def login_page(page, base_url) -> LoginPage:
    return LoginPage(page, base_url)


@pytest.fixture(scope="function")
def department_page(authenticated_page, base_url) -> DepartmentPage:
    dp = DepartmentPage(authenticated_page, base_url)
    dp.open()
    return dp


@pytest.fixture(scope="function")
def group_page(authenticated_page, base_url) -> GroupPage:
    gp = GroupPage(authenticated_page, base_url)
    gp.open()
    return gp
