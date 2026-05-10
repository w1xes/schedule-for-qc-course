"""
Task 4 — SQL для тестування (4 бали)

Demonstrates:
  - Setup   : inserting test data directly into the DB before running the test
  - Verify  : querying the DB after a UI action to confirm the expected state
  - Cleanup : removing test data after each test (via pytest fixtures)

All tests use the DBHelper (helpers/db_helper.py) and DepartmentPage PO.
"""
import pytest

from pages.department_page import DepartmentPage
from helpers.db_helper import DBHelper


# ===========================================================================
# Helper / shared constants
# ===========================================================================

SETUP_DEPT = "SQL_Setup_Department"
UI_CREATED_DEPT = "SQL_UICreated_Department"
UI_DELETED_DEPT = "SQL_UIDeleted_Department"
UI_UPDATED_DEPT_OLD = "SQL_UIUpdated_Old"
UI_UPDATED_DEPT_NEW = "SQL_UIUpdated_New"


# ===========================================================================
# Task 4 tests
# ===========================================================================

@pytest.mark.db
class TestDepartmentDBVerification:
    """
    Tests that combine UI actions with direct database assertions to ensure
    the backend persists changes correctly.
    """

    # -----------------------------------------------------------------------
    # Fixture: clean up all test department names before AND after each test
    # -----------------------------------------------------------------------
    @pytest.fixture(autouse=True)
    def db_cleanup(self, db: DBHelper):
        names_to_clean = [
            SETUP_DEPT,
            UI_CREATED_DEPT,
            UI_DELETED_DEPT,
            UI_UPDATED_DEPT_OLD,
            UI_UPDATED_DEPT_NEW,
        ]
        for name in names_to_clean:
            db.delete_department_by_name(name)
        yield
        for name in names_to_clean:
            db.delete_department_by_name(name)

    # -----------------------------------------------------------------------
    # Setup test: seed via DB, verify via UI
    # -----------------------------------------------------------------------
    def test_db_seeded_department_appears_in_ui(
        self, db: DBHelper, department_page: DepartmentPage
    ):
        """
        Setup: Insert a department directly into the DB.
        Verify: Reload the page and confirm the department card is visible in
                the UI — proving that the backend reads from the same table.
        """
        # --- Setup ---
        created = db.create_department(SETUP_DEPT)
        assert created["name"] == SETUP_DEPT, "DB insert must return the new row"

        # --- Verify in UI ---
        department_page.page.reload()
        department_page.page.wait_for_selector(
            f".department-card__name:has-text('{SETUP_DEPT}')",
            timeout=10_000,
        )
        assert department_page.is_department_visible(SETUP_DEPT), (
            f"Department '{SETUP_DEPT}' seeded via DB must appear in the UI"
        )

        # --- Verify in DB ---
        row = db.get_department_by_name(SETUP_DEPT)
        assert row is not None, "Department must exist in DB after seed"
        assert row["disable"] is False, "Newly seeded department must not be disabled"

    # -----------------------------------------------------------------------
    # UI create → DB verify
    # -----------------------------------------------------------------------
    def test_ui_create_department_persists_to_db(
        self, db: DBHelper, department_page: DepartmentPage
    ):
        """
        UI action: Create a new department through the web form.
        DB verify: Query the database and confirm the row was inserted with
                   the correct name and disable=false.
        """
        # Ensure no stale row from a previous run
        assert not db.department_exists(UI_CREATED_DEPT), "Precondition: dept must not exist"

        # --- UI action ---
        department_page.fill_name(UI_CREATED_DEPT)
        department_page.submit_form()
        department_page.page.wait_for_selector(
            f".department-card__name:has-text('{UI_CREATED_DEPT}')",
            timeout=10_000,
        )

        # --- DB verification ---
        row = db.get_department_by_name(UI_CREATED_DEPT)
        assert row is not None, (
            f"Department '{UI_CREATED_DEPT}' created through UI must exist in DB"
        )
        assert row["name"] == UI_CREATED_DEPT
        assert row["disable"] is False, "Newly created department must not be disabled"

    # -----------------------------------------------------------------------
    # UI delete → DB verify
    # -----------------------------------------------------------------------
    def test_ui_delete_department_removes_from_db(
        self, db: DBHelper, department_page: DepartmentPage
    ):
        """
        Setup: Seed a department directly into the DB.
        UI action: Delete it via the web UI and confirm the dialog.
        DB verify: The row must no longer exist in the database.
        """
        # --- Setup ---
        db.create_department(UI_DELETED_DEPT)
        assert db.department_exists(UI_DELETED_DEPT), "Precondition: dept must exist before delete"

        # --- UI action ---
        department_page.page.reload()
        department_page.page.wait_for_selector(
            f".department-card__name:has-text('{UI_DELETED_DEPT}')",
            timeout=10_000,
        )
        department_page.click_delete(UI_DELETED_DEPT)
        department_page.confirm_dialog()
        department_page.page.wait_for_selector(
            f".department-card__name:has-text('{UI_DELETED_DEPT}')",
            state="detached",
            timeout=10_000,
        )

        # --- DB verification ---
        row = db.get_department_by_name(UI_DELETED_DEPT)
        assert row is None, (
            f"Department '{UI_DELETED_DEPT}' deleted through UI must be removed from DB"
        )

    # -----------------------------------------------------------------------
    # UI edit/update → DB verify
    # -----------------------------------------------------------------------
    def test_ui_update_department_name_persists_to_db(
        self, db: DBHelper, department_page: DepartmentPage
    ):
        """
        Setup: Seed a department with the old name.
        UI action: Edit it, change the name, and save.
        DB verify: The row must exist with the new name; the old name must be gone.
        """
        # --- Setup ---
        db.create_department(UI_UPDATED_DEPT_OLD)

        # --- UI action ---
        department_page.page.reload()
        department_page.page.wait_for_selector(
            f".department-card__name:has-text('{UI_UPDATED_DEPT_OLD}')",
            timeout=10_000,
        )
        department_page.click_edit(UI_UPDATED_DEPT_OLD)
        department_page.page.wait_for_timeout(500)

        department_page.fill_name(UI_UPDATED_DEPT_NEW)
        department_page.submit_form()
        department_page.page.wait_for_selector(
            f".department-card__name:has-text('{UI_UPDATED_DEPT_NEW}')",
            timeout=10_000,
        )

        # --- DB verification ---
        new_row = db.get_department_by_name(UI_UPDATED_DEPT_NEW)
        assert new_row is not None, (
            f"Updated name '{UI_UPDATED_DEPT_NEW}' must exist in DB after UI edit"
        )

        old_row = db.get_department_by_name(UI_UPDATED_DEPT_OLD)
        assert old_row is None, (
            f"Old name '{UI_UPDATED_DEPT_OLD}' must not exist in DB after rename"
        )

    # -----------------------------------------------------------------------
    # Cleanup-only test: verify DB helper cleanup is effective
    # -----------------------------------------------------------------------
    def test_db_cleanup_removes_test_data(self, db: DBHelper):
        """
        Verify that DBHelper.delete_department_by_name() correctly removes a
        row so subsequent tests start with a clean slate.
        """
        cleanup_name = "Cleanup_Verification_Dept"
        db.delete_department_by_name(cleanup_name)  # ensure not present

        # Insert
        db.create_department(cleanup_name)
        assert db.department_exists(cleanup_name), "Dept must exist after create"

        # Cleanup
        db.delete_department_by_name(cleanup_name)
        assert not db.department_exists(cleanup_name), (
            "Dept must not exist after delete_department_by_name()"
        )
