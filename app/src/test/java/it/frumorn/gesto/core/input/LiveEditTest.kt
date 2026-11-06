package it.frumorn.gesto.core.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveEditTest {
    @Test fun append() = assertEquals(LiveEdit(0, "c"), liveEditOps("ab", "abc"))

    @Test fun delete() = assertEquals(LiveEdit(1, ""), liveEditOps("abc", "ab"))

    @Test fun suggestion_replaces_only_the_tail() = assertEquals(LiveEdit(1, "lo "), liveEditOps("helo", "hello "))

    @Test fun full_replace() = assertEquals(LiveEdit(3, "xyz"), liveEditOps("abc", "xyz"))

    @Test fun unchanged() = assertTrue(liveEditOps("abc", "abc").isEmpty)

    @Test fun clearing_deletes_everything() = assertEquals(LiveEdit(3, ""), liveEditOps("abc", ""))

    @Test fun typing_from_empty() = assertEquals(LiveEdit(0, "hi"), liveEditOps("", "hi"))

    @Test fun an_emoji_is_not_cut_in_half() {
        // "a😀" -> "a😁": both emoji share the high surrogate.
        assertEquals(LiveEdit(2, "😁"), liveEditOps("a😀", "a😁"))
    }
}
