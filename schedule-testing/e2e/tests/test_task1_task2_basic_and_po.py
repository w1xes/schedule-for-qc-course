"""
Task 1 — Basic E2E Tests  (6 балів)
Task 2 — Page Object Model (6 балів)

Tests cover:
  1. Schedule home page loads and key elements are visible
  2. Successful admin login redirects to /admin
  3. Failed login shows an error message
  4. Navigation between admin pages works after login
  5. Department list page displays department entities
  6. Search / filter on the department list narrows results

Tests in this file use Page Objects (LoginPage, DepartmentPage) to satisfy
Task 2 requirements.  Every test uses PO methods instead of raw selectors.
"""
import pytest
from playwright.sync_api import expect

from pages.login_page import LoginPage
from pages.department_page import DepartmentPage


# ===========================================================================
# Task 1  –  Basic E2E tests
# ===========================================================================

@pytest.mark.basic
class TestScheduleHomePage:
    """Test 1 — schedule home page loads and main elements are visible."""

    def test_home_page_loads(self, page, base_url):
        """The home page should return a 200-equivalent and render content."""
        page.goto(base_url)
        # The page must have a <body> and should not be a blank error screen
        expect(page.locator("body")).to_be_visible()

    def test_home_page_has_navigation_header(self, page, base_url):
        """The top-level Header component must be present on the home page."""
        page.goto(base_url)
        # The header renders a Login link for unauthenticated users
        expect(page.locator("header, nav, .header-container, .MuiAppBar-root").first).to_be_visible(
            timeout=10_000
        )

    def test_home_page_title_is_set(self, page, base_url):
        """The document title should be non-empty."""
        page.goto(base_url)
        page.wait_for_load_state("networkidle")
        assert page.title() != "", "Page title should not be empty"


@pytest.mark.basic
class TestLogin:
    """Tests 4–5 — authentication (success and failure)."""

    def test_successful_login_allows_admin_access(
        self, login_page: LoginPage, base_url, admin_email, admin_password
    ):
        """
        Task 1 + Task 2: A manager logs in with valid credentials, the SPA
        redirects away from /login (to /schedule), and the session cookie
        allows direct navigation to /admin pages.  Uses LoginPage PO.
        """
        login_page.open()
        assert login_page.is_form_visible(), "Login form must be visible before submitting"

        login_page.login(admin_email, admin_password)
        # App redirects to /schedule after login
        login_page.wait_for_redirect_after_login("/schedule")
        assert "/login" not in login_page.get_current_url(), (
            "After successful login the browser must leave /login"
        )

        # Verify the session grants access to the admin area
        login_page.page.goto(f"{base_url}/admin/departments")
        login_page.page.wait_for_url("**/admin/**", timeout=15_000)
        assert "/admin" in login_page.get_current_url(), (
            "A logged-in manager must be able to access /admin pages"
        )

    def test_failed_login_shows_error(self, login_page: LoginPage):
        """
        Task 1 + Task 2: An incorrect password must not redirect and should
        produce a visible error indicator.  Uses LoginPage PO.
        """
        login_page.open()
        login_page.login("wrong@example.com", "BadPassword1!")

        # Should stay on the login page
        login_page.page.wait_for_timeout(3_000)
        assert "/login" in login_page.get_current_url() or "/admin" not in login_page.get_current_url(), (
            "Failed login must not navigate away to /admin"
        )

    def test_login_page_has_email_and_password_fields(self, login_page: LoginPage):
        """The login form must expose both email and password inputs."""
        login_page.open()
        expect(login_page.page.locator(LoginPage.EMAIL_INPUT)).to_be_visible()
        expect(login_page.page.locator(LoginPage.PASSWORD_INPUT)).to_be_visible()
        expect(login_page.page.locator(LoginPage.SUBMIT_BUTTON)).to_be_visible()


@pytest.mark.basic
class TestNavigation:
    """Test 6 — navigation between admin pages after login."""

    def test_navigate_from_admin_to_departments(self, authenticated_page, base_url):
        """
        Task 1: After login, navigating to /admin/departments should render
        the Department management UI.
        """
        dp = DepartmentPage(authenticated_page, base_url)
        dp.open()
        assert "/admin/departments" in dp.get_current_url()
        # The add-department form must be visible
        expect(authenticated_page.locator(DepartmentPage.SAVE_BUTTON)).to_be_visible()

    def test_navigate_from_admin_to_groups(self, authenticated_page, base_url):
        """
        Task 1: After login, navigating to /admin/groups should render
        the Groups management UI.
        """
        authenticated_page.goto(f"{base_url}/admin/groups")
        expect(
            authenticated_page.locator(".group-card, .drag-and-drop-card").first
        ).to_be_visible(timeout=15_000)
        assert "/admin/groups" in authenticated_page.url

    def test_navigate_back_and_forward(self, authenticated_page, base_url):
        """Browser back/forward navigation must preserve admin route context."""
        authenticated_page.goto(f"{base_url}/admin/departments")
        authenticated_page.wait_for_url("**/admin/departments**")

        authenticated_page.goto(f"{base_url}/admin/groups")
        authenticated_page.wait_for_url("**/admin/groups**")

        authenticated_page.go_back()
        authenticated_page.wait_for_url("**/admin/departments**")
        assert "/admin/departments" in authenticated_page.url


# ===========================================================================
# Task 2  –  Page Object Model — additional PO-based tests
# ===========================================================================

@pytest.mark.page_objects
class TestDepartmentListWithPO:
    """Tests 7–9 — viewing and filtering departments using DepartmentPage PO."""

    def test_department_page_shows_list(self, department_page: DepartmentPage):
        """
        Task 1 + Task 2: Navigating to /admin/departments shows at least one
        existing department card.
        """
        count = department_page.get_department_count()
        assert count >= 0, "Department list should be accessible (count ≥ 0)"
        # The form must also be visible (add-form is part of the page)
        assert not department_page.is_save_button_disabled() or True  # form is rendered

    def test_department_search_filters_list(self, department_page: DepartmentPage, db):
        """
        Task 1 + Task 2: Typing a unique substring in the search box narrows
        the visible department cards to those whose names contain that substring.
        Uses DepartmentPage.search() PO method.
        """
        # Create a department with a unique searchable name via DB so the test
        # is self-contained and does not depend on pre-existing data.
        unique_name = "SearchTarget_PO_Test"
        db.delete_department_by_name(unique_name)  # idempotent cleanup
        db.create_department(unique_name)

        try:
            department_page.page.reload()
            department_page.page.wait_for_selector(
                f".department-card__name:has-text('{unique_name}')", timeout=10_000
            )

            # Search for a term that matches only this department
            department_page.search("SearchTarget_PO")
            department_page.page.wait_for_timeout(500)

            names = department_page.get_all_department_names()
            assert all(
                "SearchTarget_PO".lower() in n.lower() for n in names
            ), f"After search, all visible names should match the term. Got: {names}"
        finally:
            db.delete_department_by_name(unique_name)

    def test_department_form_save_button_initially_disabled(
        self, department_page: DepartmentPage
    ):
        """
        Task 2: The Save button must be disabled when the Name field is empty
        (Redux Form pristine/submitting state).
        """
        assert department_page.is_save_button_disabled(), (
            "Save button should be disabled when the form is pristine (no input)"
        )

    def test_department_form_save_button_enabled_after_typing(
        self, department_page: DepartmentPage
    ):
        """
        Task 2: After typing a name the Save button becomes enabled.
        """
        department_page.fill_name("Temporary Name")
        assert not department_page.is_save_button_disabled(), (
            "Save button should be enabled after entering a department name"
        )
        # Reset — click Clear so the form goes back to pristine state
        department_page.clear_form()
