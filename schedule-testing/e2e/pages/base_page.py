"""
Base Page Object with common browser interactions.
"""
from playwright.sync_api import Page, expect


class BasePage:
    """Base class for all Page Objects."""

    def __init__(self, page: Page, base_url: str):
        self.page = page
        self.base_url = base_url

    def navigate(self, path: str = "") -> None:
        """Navigate to a URL relative to base_url."""
        self.page.goto(f"{self.base_url}{path}")

    def get_current_url(self) -> str:
        return self.page.url

    def wait_for_url(self, path: str, timeout: int = 10_000) -> None:
        self.page.wait_for_url(f"**{path}", timeout=timeout)

    def wait_for_network_idle(self, timeout: int = 15_000) -> None:
        self.page.wait_for_load_state("networkidle", timeout=timeout)

    def is_visible(self, selector: str) -> bool:
        return self.page.locator(selector).is_visible()

    def get_text(self, selector: str) -> str:
        return self.page.locator(selector).inner_text()
