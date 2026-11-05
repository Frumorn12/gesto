package it.frumorn.gesto.core.model

import it.frumorn.gesto.ui.text.UiText

enum class NoticeAction { Retry, OpenPcs, UseCode }

/** A short message for the user, shown once (snackbar). */
data class Notice(val text: UiText, val action: NoticeAction? = null, val id: Long = System.nanoTime())
