"""
Page Object for the Department administration page (/admin/departments).

Maps to the DepartmentPage React component and AddDepartmentForm.
"""
from playwright.sync_api import Page, expect, Locator

from .base_page import BasePage


class DepartmentPage(BasePage):
    """
    Encapsulates all interactions with the Department management page.

    UI structure (from DepartmentPage.js / AddDepartmentForm.js):
      - .search-list__panel            : left sidebar containing the search box and the add/edit form
      - .form-card.subject-form         : the add/edit department card
      - input[name="name"]             : department name text field
      - button[type="submit"]          : Save button
      - .department-card               : each individual department card in the list
      - .department-card__name         : the visible department name inside a card
      - .edit-btn                      : pencil / edit icon button
      - .delete-btn                    : trash / delete icon button
      - .MuiDialog-root                : confirmation dialog (Material-UI)
    """

    DEPARTMENTS_PATH = "/admin/departments"

    # Form selectors
    NAME_INPUT = 'input[name="name"]'
    SAVE_BUTTON = '.subject-form button[type="submit"]'
    CANCEL_CLEAR_BUTTON = '.form-buttons-container button:not([type="submit"])'

    # Department list selectors
    DEPARTMENT_CARD = ".department-card"
    DEPARTMENT_NAME = ".department-card__name"
    EDIT_BTN = ".edit-btn"
    DELETE_BTN = ".delete-btn"

    # Search panel
    SEARCH_INPUT = 'input[type="text"].MuiInputBase-input'

    # Confirmation dialog
    CONFIRM_DIALOG = ".MuiDialog-root"
    CONFIRM_OK_BUTTON = ".MuiDialog-root button:has-text('Yes'), .MuiDialog-root button:has-text('Так')"

    def __init__(self, page: Page, base_url: str):
        super().__init__(page, base_url)

    # ------------------------------------------------------------------
    # Navigation
    # ------------------------------------------------------------------
    def open(self) -> "DepartmentPage":
        """Navigate to the departments page and wait for the form to appear."""
        self.navigate(self.DEPARTMENTS_PATH)
        # Wait for the name input (most reliable indicator that the form loaded)
        expect(self.page.locator(self.NAME_INPUT)).to_be_visible(timeout=15_000)
        return self

    # ------------------------------------------------------------------
    # Form interactions
    # ------------------------------------------------------------------
    def fill_name(self, name: str) -> "DepartmentPage":
        """Clear and type a department name into the Name field."""
        field = self.page.locator(self.NAME_INPUT)
        field.clear()
        field.fill(name)
        return self

    def submit_form(self) -> "DepartmentPage":
        """Click the Save button."""
        self.page.locator(self.SAVE_BUTTON).click()
        return self

    def clear_form(self) -> "DepartmentPage":
        """Click the Clear / Cancel button."""
        self.page.locator(self.CANCEL_CLEAR_BUTTON).click()
        return self

    def create_department(self, name: str) -> "DepartmentPage":
        """Fill the form and save a new department, then wait for the list to update."""
        self.fill_name(name)
        self.submit_form()
        # Wait until the new card appears in the list
        self.page.wait_for_selector(
            f".department-card__name:has-text('{name}')",
            timeout=10_000,
        )
        return self

    # ------------------------------------------------------------------
    # List interactions
    # ------------------------------------------------------------------
    def get_all_department_names(self) -> list[str]:
        """Return all visible department name strings from the list."""
        cards = self.page.locator(self.DEPARTMENT_NAME).all()
        return [card.inner_text().strip() for card in cards]

    def get_department_count(self) -> int:
        return self.page.locator(self.DEPARTMENT_CARD).count()

    def find_department_card(self, name: str) -> Locator:
        """Return the card locator for a department with the given exact name."""
        return self.page.locator(self.DEPARTMENT_CARD).filter(
            has=self.page.locator(f".department-card__name:text-is('{name}')")
        )

    def click_edit(self, name: str) -> "DepartmentPage":
        """Click the edit icon for the department with the given name."""
        card = self.find_department_card(name)
        card.locator(self.EDIT_BTN).click()
        return self

    def click_delete(self, name: str) -> "DepartmentPage":
        """Click the delete icon for the department with the given name.
        
        Note: the department card has two SVGs with class .delete-btn
        (MdDelete for dept deletion and FaChalkboardTeacher for showing teachers).
        We always click the first one which is the actual delete action.
        """
        card = self.find_department_card(name)
        card.locator(self.DELETE_BTN).first.click()
        return self

    def confirm_dialog(self) -> "DepartmentPage":
        """Accept the confirmation dialog (Yes / Так button)."""
        self.page.wait_for_selector(self.CONFIRM_DIALOG, timeout=5_000)
        self.page.locator(self.CONFIRM_OK_BUTTON).click()
        return self

    # ------------------------------------------------------------------
    # Search
    # ------------------------------------------------------------------
    def search(self, term: str) -> "DepartmentPage":
        """Type into the search panel input."""
        self.page.locator(self.SEARCH_INPUT).first.fill(term)
        return self

    def clear_search(self) -> "DepartmentPage":
        self.page.locator(self.SEARCH_INPUT).first.fill("")
        return self

    # ------------------------------------------------------------------
    # State queries
    # ------------------------------------------------------------------
    def is_department_visible(self, name: str) -> bool:
        """Return True if a department card with the exact given name is in the DOM.
        
        Uses :text-is() for exact (not substring) matching so that
        'E2E_Dept' does not match 'E2E_Dept_Updated'.
        """
        return (
            self.page.locator(f".department-card__name:text-is('{name}')").count() > 0
        )

    def is_save_button_disabled(self) -> bool:
        return self.page.locator(self.SAVE_BUTTON).is_disabled()

    def get_name_field_value(self) -> str:
        return self.page.locator(self.NAME_INPUT).input_value()

    def get_form_title(self) -> str:
        return self.page.locator(".form-card.subject-form h2").inner_text().strip()
