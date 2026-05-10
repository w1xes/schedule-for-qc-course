"""
Database helper for E2E test setup, verification, and cleanup.

Uses psycopg2 to connect directly to the PostgreSQL instance used by the
running application.  Connection parameters are taken from environment
variables (see .env).
"""
import os
from contextlib import contextmanager
from typing import Any

import psycopg2
import psycopg2.extras


class DBHelper:
    """Thin wrapper around psycopg2 for use in E2E tests."""

    def __init__(
        self,
        host: str = "localhost",
        port: int = 5432,
        dbname: str = "appdb",
        user: str = "postgres",
        password: str = "postgres",
    ):
        self._dsn = {
            "host": host,
            "port": port,
            "dbname": dbname,
            "user": user,
            "password": password,
        }

    @contextmanager
    def _connection(self):
        conn = psycopg2.connect(**self._dsn)
        try:
            yield conn
            conn.commit()
        except Exception:
            conn.rollback()
            raise
        finally:
            conn.close()

    # ------------------------------------------------------------------
    # Generic helpers
    # ------------------------------------------------------------------
    def execute(self, sql: str, params: tuple = ()) -> None:
        """Execute a DML statement (INSERT / UPDATE / DELETE)."""
        with self._connection() as conn:
            with conn.cursor() as cur:
                cur.execute(sql, params)

    def fetchone(self, sql: str, params: tuple = ()) -> dict | None:
        """Return the first row as a dict, or None."""
        with self._connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute(sql, params)
                return cur.fetchone()

    def fetchall(self, sql: str, params: tuple = ()) -> list[dict]:
        """Return all rows as a list of dicts."""
        with self._connection() as conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute(sql, params)
                return cur.fetchall()

    # ------------------------------------------------------------------
    # Department helpers
    # ------------------------------------------------------------------
    def get_department_by_name(self, name: str) -> dict | None:
        """Return a department row dict, or None if not found."""
        return self.fetchone(
            "SELECT id, name, disable FROM department WHERE name = %s",
            (name,),
        )

    def department_exists(self, name: str) -> bool:
        row = self.get_department_by_name(name)
        return row is not None

    def create_department(self, name: str, disable: bool = False) -> dict:
        """Insert a department directly and return the new row."""
        row = self.fetchone(
            """
            INSERT INTO department (name, disable)
            VALUES (%s, %s)
            RETURNING id, name, disable
            """,
            (name, disable),
        )
        return dict(row)

    def delete_department_by_name(self, name: str) -> None:
        """Hard-delete a department by name (use for test cleanup)."""
        self.execute("DELETE FROM department WHERE name = %s", (name,))

    def delete_department_by_id(self, dept_id: int) -> None:
        self.execute("DELETE FROM department WHERE id = %s", (dept_id,))

    def get_all_departments(self) -> list[dict]:
        return self.fetchall(
            "SELECT id, name, disable FROM department ORDER BY id"
        )

    # ------------------------------------------------------------------
    # Group helpers
    # ------------------------------------------------------------------
    def get_all_groups_ordered(self) -> list[dict]:
        """Return enabled groups in the sort order stored in the DB."""
        return self.fetchall(
            "SELECT id, title, sort_order FROM groups WHERE disable = false ORDER BY sort_order"
        )

    def get_group_by_title(self, title: str) -> dict | None:
        return self.fetchone(
            "SELECT id, title, sort_order FROM groups WHERE title = %s",
            (title,),
        )


def make_db_helper() -> DBHelper:
    """Create a DBHelper from environment variables."""
    return DBHelper(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "5432")),
        dbname=os.getenv("DB_NAME", "appdb"),
        user=os.getenv("DB_USER", "postgres"),
        password=os.getenv("DB_PASSWORD", "postgres"),
    )
