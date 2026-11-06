package it.frumorn.gesto.core.model

import it.frumorn.gesto.ui.text.UiText

enum class NoticeAction { Retry, OpenPcs, UseCode }

/** A short message for the user, shown once (snackbar). */
data class Notice(val text: UiText, val action: NoticeAction? = null, val id: Long = System.nanoTime())

/** Identical notices within this window are shown once (e.g. a failure repeated per keystroke). */
const val NOTICE_DEDUPE_MS = 3_000L

/**
 * Whether [next] is shown, given the last notice shown. An identical one within [NOTICE_DEDUPE_MS]
 * is dropped, but never one that offers an action: the user may just have tapped it ("Try again")
 * and must get an answer even when it is the same.
 */
fun shouldEmitNotice(previous: Notice?, previousAtMs: Long, next: Notice, nowMs: Long): Boolean =
    next.action != null ||
        previous == null ||
        next.text != previous.text ||
        nowMs - previousAtMs >= NOTICE_DEDUPE_MS
