"""
Task 3 — Складні сценарії (4 бали)

Tests:
  1. Full CRUD for Department through the UI (create → view → edit → delete)
  2. Drag & Drop — reorder groups in the schedule view
  3. Form validation — required fields and duplicate name rejection
"""
import pytest
from playwright.sync_api import expect

from pages.department_page import DepartmentPage
from pages.group_page import GroupPage


# ===========================================================================
# 3.1  CRUD — Department (кафедра) via UI
# ===========================================================================

@pytest.mark.complex
class TestDepartmentCRUD:
    """
    Full create → view → edit → delete lifecycle for a Department entity
    through the web form.  Each step asserts the expected UI state.
    """

    DEPT_NAME = "E2E_CRUD_Department"
    DEPT_NAME_UPDATED = "E2E_CRUD_Department_Updated"

    @pytest.fixture(autouse=True)
    def cleanup(self, db):
        """Ensure test departments are removed before and after the test."""
        db.delete_department_by_name(self.DEPT_NAME)
        db.delete_department_by_name(self.DEPT_NAME_UPDATED)
        yield
        db.delete_department_by_name(self.DEPT_NAME)
        db.delete_department_by_name(self.DEPT_NAME_UPDATED)

    def test_create_department_via_form(self, department_page: DepartmentPage):
        """
        Step 1 – Create: Type a department name and click Save.
        The new card must appear in the list immediately.
        """
        department_page.fill_name(self.DEPT_NAME)
        assert not department_page.is_save_button_disabled(), (
            "Save button must be enabled after entering a name"
        )
        department_page.submit_form()

        # Verify the card appeared in the list
        department_page.page.wait_for_selector(
            f".department-card__name:has-text('{self.DEPT_NAME}')",
            timeout=10_000,
        )
        assert department_page.is_department_visible(self.DEPT_NAME), (
            f"Department '{self.DEPT_NAME}' should be visible after creation"
        )

    def test_view_created_department(self, department_page: DepartmentPage, db):
        """
        Step 2 – View: After creation via DB seed, reload the page and confirm
        the department is rendered in the list.
        """
        db.create_department(self.DEPT_NAME)

        department_page.page.reload()
        department_page.page.wait_for_selector(
            f".department-card__name:has-text('{self.DEPT_NAME}')",
            timeout=10_000,
        )
        names = department_page.get_all_department_names()
        assert self.DEPT_NAME in names, (
            f"'{self.DEPT_NAME}' must appear in department list. Got: {names}"
        )

    def test_edit_department_name(self, department_page: DepartmentPage, db):
        """
        Step 3 – Edit: Click the edit icon on a department card, change the
        name in the form, save, and confirm the updated name is shown.
        """
        db.create_department(self.DEPT_NAME)

        department_page.page.reload()
        department_page.page.wait_for_selector(
            f".department-card__name:has-text('{self.DEPT_NAME}')",
            timeout=10_000,
        )

        # Click edit — this loads the department data into the form
        department_page.click_edit(self.DEPT_NAME)
        department_page.page.wait_for_timeout(500)  # wait for form to populate

        # The form Name field must now contain the original name
        current_value = department_page.get_name_field_value()
        assert self.DEPT_NAME in current_value, (
            f"Form name field should contain '{self.DEPT_NAME}', got '{current_value}'"
        )

        # Update the name
        department_page.fill_name(self.DEPT_NAME_UPDATED)
        department_page.submit_form()

        # Wait for updated card to appear
        department_page.page.wait_for_selector(
            f".department-card__name:has-text('{self.DEPT_NAME_UPDATED}')",
            timeout=10_000,
        )
        assert department_page.is_department_visible(self.DEPT_NAME_UPDATED), (
            f"Updated name '{self.DEPT_NAME_UPDATED}' must be visible in the list"
        )
        assert not department_page.is_department_visible(self.DEPT_NAME), (
            f"Old name '{self.DEPT_NAME}' must no longer be visible"
        )

    def test_delete_department(self, department_page: DepartmentPage, db):
        """
        Step 4 – Delete: Click the delete icon, confirm the dialog, and verify
        the card disappears from the list.
        """
        db.create_department(self.DEPT_NAME)

        department_page.page.reload()
        department_page.page.wait_for_selector(
            f".department-card__name:has-text('{self.DEPT_NAME}')",
            timeout=10_000,
        )

        department_page.click_delete(self.DEPT_NAME)
        department_page.confirm_dialog()

        # The card should disappear
        department_page.page.wait_for_selector(
            f".department-card__name:has-text('{self.DEPT_NAME}')",
            state="detached",
            timeout=10_000,
        )
        assert not department_page.is_department_visible(self.DEPT_NAME), (
            f"Deleted department '{self.DEPT_NAME}' must not be visible in the list"
        )


# ===========================================================================
# 3.2  Drag & Drop — reorder groups in the schedule
# ===========================================================================

@pytest.mark.complex
class TestGroupDragAndDrop:
    """
    Drag a group card and drop it onto another group card to change the
    display order.  After the drop, the API call completes and the UI
    reflects the new order.
    """

    def test_drag_group_after_another(self, group_page: GroupPage):
        """
        Drag the first group and drop it after the second group.
        The resulting order must differ from the original order.
        """
        original_order = group_page.get_all_group_titles()

        # Need at least 2 groups for a meaningful drag-and-drop test
        if len(original_order) < 2:
            pytest.skip("Need at least 2 enabled groups for drag-and-drop test")

        source_title = original_order[0]
        target_title = original_order[1]

        group_page.drag_group_after(source_title, target_title)
        group_page.page.wait_for_timeout(1_000)

        new_order = group_page.get_all_group_titles()

        assert new_order != original_order or len(original_order) == 1, (
            f"The group order should change after drag-and-drop.\n"
            f"Before: {original_order}\n"
            f"After:  {new_order}"
        )

    def test_dragged_group_stays_in_list(self, group_page: GroupPage):
        """
        After a drag-and-drop operation no group should be lost from the list;
        the count must remain the same.
        """
        original_titles = group_page.get_all_group_titles()

        if len(original_titles) < 2:
            pytest.skip("Need at least 2 enabled groups for this test")

        count_before = len(original_titles)
        group_page.drag_group_after(original_titles[0], original_titles[-1])
        group_page.page.wait_for_timeout(1_000)

        count_after = group_page.get_group_count()
        assert count_after == count_before, (
            f"Group count must not change after drag-and-drop: "
            f"{count_before} → {count_after}"
        )


# ===========================================================================
# 3.3  Form Validation
# ===========================================================================

@pytest.mark.complex
class TestDepartmentFormValidation:
    """
    Verify that the AddDepartmentForm enforces required-field and
    duplicate-name validation constraints.
    """

    DUPLICATE_NAME = "E2E_Duplicate_Dept"

    @pytest.fixture(autouse=True)
    def cleanup(self, db):
        db.delete_department_by_name(self.DUPLICATE_NAME)
        yield
        db.delete_department_by_name(self.DUPLICATE_NAME)

    def test_save_button_disabled_when_name_is_empty(
        self, department_page: DepartmentPage
    ):
        """
        The Save button must be disabled (pristine form) when the Name field
        is empty — required field validation at Redux Form level.
        """
        # Ensure the name field is empty
        department_page.page.locator(DepartmentPage.NAME_INPUT).fill("")
        department_page.page.keyboard.press("Tab")
        department_page.page.wait_for_timeout(300)

        assert department_page.is_save_button_disabled(), (
            "Save button must be disabled when the Name field is empty"
        )

    def test_duplicate_name_prevents_save(
        self, department_page: DepartmentPage, db
    ):
        """
        Entering a name that already exists should trigger the uniqueDepartment
        validator, which disables the Save button (or shows an error).
        """
        db.create_department(self.DUPLICATE_NAME)
        department_page.page.reload()
        department_page.page.wait_for_load_state("networkidle")

        department_page.fill_name(self.DUPLICATE_NAME)
        department_page.page.keyboard.press("Tab")  # trigger onBlur validation
        department_page.page.wait_for_timeout(500)

        # Either the Save button is disabled OR a validation error is shown
        save_disabled = department_page.is_save_button_disabled()
        error_visible = (
            department_page.page.locator(".MuiFormHelperText-root").count() > 0
        )
        assert save_disabled or error_visible, (
            "Entering a duplicate department name must disable Save or show an error message"
        )

    def test_whitespace_only_name_does_not_save(
        self, department_page: DepartmentPage
    ):
        """
        A whitespace-only name must not create a new department.

        The frontend Redux Form does not distinguish whitespace from real text
        (it only checks truthiness), so the Save button may be enabled.
        However, the entity carries @NotBlank which causes Hibernate to reject
        the value at persist time (ConstraintViolationException -> 4xx/5xx).
        We therefore verify that submitting such a form does NOT increase the
        department count.
        """
        count_before = department_page.get_department_count()

        department_page.fill_name("   ")
        department_page.page.keyboard.press("Tab")
        department_page.page.wait_for_timeout(300)

        if department_page.is_save_button_disabled():
            # Frontend validation caught it — no further action needed
            return

        # Frontend allows the submit; send it and verify the server rejects it
        department_page.submit_form()
        department_page.page.wait_for_timeout(1_500)

        count_after = department_page.get_department_count()
        assert count_after == count_before, (
            f"Whitespace-only name must not create a new department "
            f"(count was {count_before}, is now {count_after})"
        )
