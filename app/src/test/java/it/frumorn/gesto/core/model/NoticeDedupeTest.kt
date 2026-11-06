package it.frumorn.gesto.core.model

import it.frumorn.gesto.ui.text.UiText
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoticeDedupeTest {
    private val failed = Notice(UiText.Raw("Something went wrong"))
    private val notAnswering = Notice(UiText.Raw("frumorn isn't answering."), NoticeAction.Retry)

    @Test fun first_notice_is_shown() {
        assertTrue(shouldEmitNotice(previous = null, previousAtMs = 0, next = failed, nowMs = 1_000))
    }

    @Test fun same_text_within_the_window_is_dropped() {
        assertFalse(shouldEmitNotice(failed, previousAtMs = 1_000, next = failed.copy(id = 2), nowMs = 3_999))
    }

    @Test fun same_text_after_the_window_is_shown() {
        assertTrue(shouldEmitNotice(failed, previousAtMs = 1_000, next = failed.copy(id = 2), nowMs = 1_000 + NOTICE_DEDUPE_MS))
    }

    @Test fun different_text_is_shown() {
        val other = Notice(UiText.Raw("Connection restored"))
        assertTrue(shouldEmitNotice(failed, previousAtMs = 1_000, next = other, nowMs = 1_100))
    }

    @Test fun a_notice_with_an_action_is_never_dropped() {
        // "Try again" right after a refused connection must answer again, even with the same text.
        assertTrue(shouldEmitNotice(notAnswering, previousAtMs = 1_000, next = notAnswering.copy(id = 2), nowMs = 1_200))
    }
}
