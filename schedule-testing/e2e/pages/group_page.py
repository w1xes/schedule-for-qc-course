"""
Page Object for the Groups administration page (/admin/groups).

Covers the GroupList component and DraggableCard drag-and-drop functionality.
"""
from playwright.sync_api import Page, expect, Locator

from .base_page import BasePage


class GroupPage(BasePage):
    """
    Encapsulates interactions with the Groups management page.

    UI structure (from GroupPage.js / GroupList.js / DraggableCard.js):
      - .group-card                 : each group card wrapper (inside drag-and-drop-card)
      - .drag-and-drop-card         : draggable wrapper for each group
      - .group-card__title          : group name / title text inside a card
      - .group-sidebar              : left sidebar (search + add form)
    """

    GROUPS_PATH = "/admin/groups"

    GROUP_CARD = ".group-card"
    DRAGGABLE_CARD = ".drag-and-drop-card"
    # The group title/number is rendered in .group-card__number (GroupCard.js)
    GROUP_TITLE = ".group-card__number"
    SEARCH_INPUT = 'input[type="text"].MuiInputBase-input'

    def __init__(self, page: Page, base_url: str):
        super().__init__(page, base_url)

    # ------------------------------------------------------------------
    # Navigation
    # ------------------------------------------------------------------
    def open(self) -> "GroupPage":
        self.navigate(self.GROUPS_PATH)
        expect(self.page.locator(self.DRAGGABLE_CARD).first).to_be_visible(timeout=10_000)
        return self

    # ------------------------------------------------------------------
    # List queries
    # ------------------------------------------------------------------
    def get_all_group_titles(self) -> list[str]:
        """Return ordered list of visible group title strings."""
        cards = self.page.locator(self.GROUP_TITLE).all()
        return [c.inner_text().strip() for c in cards]

    def get_group_count(self) -> int:
        return self.page.locator(self.DRAGGABLE_CARD).count()

    def get_draggable_card(self, title: str) -> Locator:
        """Return the draggable wrapper for the card with the given title."""
        return self.page.locator(self.DRAGGABLE_CARD).filter(
            has=self.page.locator(f".group-card__number:has-text('{title}')")
        )

    # ------------------------------------------------------------------
    # Drag & Drop
    # ------------------------------------------------------------------
    def drag_group_after(self, source_title: str, target_title: str) -> "GroupPage":
        """
        Drag the group card identified by *source_title* and drop it onto
        the card identified by *target_title*, making the source appear after
        the target in the list (matches the dragAndDropGroupStart action).
        """
        source = self.get_draggable_card(source_title)
        target = self.get_draggable_card(target_title)

        source_box = source.bounding_box()
        target_box = target.bounding_box()

        assert source_box is not None, f"Could not locate source card: {source_title}"
        assert target_box is not None, f"Could not locate target card: {target_title}"

        # Start from the centre of the source card
        sx = source_box["x"] + source_box["width"] / 2
        sy = source_box["y"] + source_box["height"] / 2

        # Drop onto the centre of the target card
        tx = target_box["x"] + target_box["width"] / 2
        ty = target_box["y"] + target_box["height"] / 2

        self.page.mouse.move(sx, sy)
        self.page.mouse.down()
        # Move in small steps so browser fires dragover events
        steps = 10
        dx = (tx - sx) / steps
        dy = (ty - sy) / steps
        for i in range(1, steps + 1):
            self.page.mouse.move(sx + dx * i, sy + dy * i)
        self.page.mouse.up()
        # Allow the re-order request to complete
        self.page.wait_for_load_state("networkidle", timeout=5_000)
        return self

    # ------------------------------------------------------------------
    # Search
    # ------------------------------------------------------------------
    def search(self, term: str) -> "GroupPage":
        self.page.locator(self.SEARCH_INPUT).first.fill(term)
        return self
