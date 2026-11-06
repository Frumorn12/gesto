package it.frumorn.gesto.core.input

/** What the PC must do to turn [previous] into the new text: [backspaces] deletions, then type [insert]. */
data class LiveEdit(val backspaces: Int, val insert: String) {
    val isEmpty: Boolean get() = backspaces == 0 && insert.isEmpty()
}

/**
 * The smallest edit between two states of the live text field. A suggestion tap or a swapped word
 * changes the middle of the text; only the part after the common prefix is deleted and retyped,
 * never the whole text again.
 */
fun liveEditOps(previous: String, next: String): LiveEdit {
    var prefix = 0
    val limit = minOf(previous.length, next.length)
    while (prefix < limit && previous[prefix] == next[prefix]) prefix++
    // Never cut a surrogate pair (emoji) in two.
    if (prefix > 0 && prefix < previous.length && Character.isHighSurrogate(previous[prefix - 1])) prefix--
    return LiveEdit(previous.length - prefix, next.substring(prefix))
}
