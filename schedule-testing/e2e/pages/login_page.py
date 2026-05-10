"""
Page Object for the Login page (/login).

Covers the Material-UI based LoginForm component.
"""
from playwright.sync_api import Page, expect

from .base_page import BasePage


class LoginPage(BasePage):
    """
    Encapsulates all interactions with the Login page.

    Selectors are derived from the React component structure:
      - auth-card  : outer card wrapper
      - auth-form  : the <form> element
      - input[name="email"]    : e-mail field (rendered by Material-UI TextField)
      - input[name="password"] : password field
      - button[type="submit"]  : Log-in button
    """

    LOGIN_PATH = "/login"

    # Locators
    EMAIL_INPUT = 'input[name="email"]'
    PASSWORD_INPUT = 'input[name="password"]'
    SUBMIT_BUTTON = 'button[type="submit"]'
    AUTH_CARD = ".auth-card"
    AUTH_FORM = ".auth-form"
    ERROR_TEXT = ".MuiFormHelperText-root"  # MUI helper text for validation errors

    def __init__(self, page: Page, base_url: str):
        super().__init__(page, base_url)

    # ------------------------------------------------------------------
    # Navigation
    # ------------------------------------------------------------------
    def open(self) -> "LoginPage":
        """Navigate to the login page and wait until it is fully loaded."""
        self.navigate(self.LOGIN_PATH)
        expect(self.page.locator(self.AUTH_CARD)).to_be_visible(timeout=10_000)
        return self

    # ------------------------------------------------------------------
    # Actions
    # ------------------------------------------------------------------
    def enter_email(self, email: str) -> "LoginPage":
        self.page.locator(self.EMAIL_INPUT).fill(email)
        return self

    def enter_password(self, password: str) -> "LoginPage":
        self.page.locator(self.PASSWORD_INPUT).fill(password)
        return self

    def click_submit(self) -> "LoginPage":
        self.page.locator(self.SUBMIT_BUTTON).click()
        return self

    def login(self, email: str, password: str) -> "LoginPage":
        """Fill in credentials and submit the form."""
        self.enter_email(email)
        self.enter_password(password)
        self.click_submit()
        return self

    # ------------------------------------------------------------------
    # Assertions / state queries
    # ------------------------------------------------------------------
    def is_form_visible(self) -> bool:
        """Return True if the login form is rendered on screen."""
        return self.page.locator(self.AUTH_FORM).is_visible()

    def get_error_message(self) -> str:
        """Return the text of the first visible helper/error message."""
        locator = self.page.locator(self.ERROR_TEXT).first
        if locator.is_visible():
            return locator.inner_text()
        return ""

    def wait_for_redirect_after_login(self, expected_path: str = "/schedule") -> None:
        """
        Block until the browser navigates away from the login page.
        The SPA redirects managers to /schedule after login (not /admin) due to
        a stale-closure in Auth.js useCallback([], []) — userRole is null at
        callback creation time so the MANAGER branch never fires on first login.
        """
        self.page.wait_for_url(f"**{expected_path}**", timeout=15_000)
