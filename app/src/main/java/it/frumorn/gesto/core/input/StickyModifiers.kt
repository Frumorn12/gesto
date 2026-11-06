package it.frumorn.gesto.core.input

/** A modifier of the keyboard sheet; [wire] is the key name the daemon knows. */
enum class ModifierKey(val wire: String) {
    Ctrl("ctrl"),
    Alt("alt"),
    Super("super"),
    Shift("shift"),
}

/**
 * The modifiers pressed on the keyboard sheet. They stay on until the next key is sent, then switch
 * off by themselves, like the sticky keys of a desktop. Declaration order of [ModifierKey] is the
 * order they are sent in, whatever order they were pressed.
 */
class StickyModifiers {
    private var on: Set<ModifierKey> = emptySet()

    val active: Set<ModifierKey>
        get() = on

    fun toggle(key: ModifierKey) {
        on = if (key in on) on - key else on + key
    }

    fun clear() {
        on = emptySet()
    }

    /**
     * Typed text while modifiers are on: the modifiers plus the first character in lower case, and
     * the modifiers switch off. Null when none is on (or nothing was typed): it is normal typing.
     */
    fun consume(typed: String): List<String>? {
        if (on.isEmpty() || typed.isEmpty()) return null
        val first = typed.codePointAt(0)
        return withModifiers(String(Character.toChars(first)).lowercase())
    }

    /** [key] (a daemon key name) after the active modifiers, which then switch off. */
    fun withModifiers(key: String): List<String> {
        val keys = ModifierKey.entries.filter { it in on }.map { it.wire } + key
        on = emptySet()
        return keys
    }
}
