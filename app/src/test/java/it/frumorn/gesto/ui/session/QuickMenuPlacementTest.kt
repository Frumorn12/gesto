package it.frumorn.gesto.ui.session

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickMenuPlacementTest {
    private fun place(button: QuickButtonBounds?, areaWidth: Int = 400, areaHeight: Int = 800) =
        placeQuickMenu(areaWidth, areaHeight, button, menuWidth = 220, menuHeight = 340, gap = 8, inset = 14)

    @Test
    fun aButtonBottomRightOpensTheMenuAboveItAlignedRight() {
        assertEquals(
            QuickMenuPlacement(x = 386 - 220, y = 740 - 8 - 340, originX = 1f, originY = 1f),
            place(QuickButtonBounds(340, 740, 46)),
        )
    }

    @Test
    fun aButtonTopLeftOpensTheMenuBelowItAlignedLeft() {
        assertEquals(
            QuickMenuPlacement(x = 14, y = 14 + 46 + 8, originX = 0f, originY = 0f),
            place(QuickButtonBounds(14, 14, 46)),
        )
    }

    @Test
    fun aLandscapeScreenTooShortForEitherOpensBesideTheButton() {
        val placement = place(QuickButtonBounds(1900, 330, 46), areaWidth = 2000, areaHeight = 390)

        assertEquals(1900 - 8 - 220, placement.x)
        assertEquals(330 + 46 - 340, placement.y)
    }

    @Test
    fun withoutAButtonTheMenuSitsBottomRight() {
        assertEquals(QuickMenuPlacement(400 - 14 - 220, 800 - 14 - 340, 1f, 1f), place(null))
    }

    @Test
    fun theMenuNeverLeavesTheArea() {
        val placement = place(QuickButtonBounds(0, 0, 46), areaWidth = 200, areaHeight = 300)

        assertEquals(0, placement.x)
        assertEquals(0, placement.y)
    }
}
