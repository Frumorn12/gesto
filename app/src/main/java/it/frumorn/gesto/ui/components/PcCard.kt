package it.frumorn.gesto.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import it.frumorn.gesto.R
import it.frumorn.gesto.core.session.Presence
import it.frumorn.gesto.ui.state.PcCardState
import it.frumorn.gesto.ui.text.UiText
import it.frumorn.gesto.ui.text.asString
import it.frumorn.gesto.ui.theme.GestoMotion
import it.frumorn.gesto.ui.theme.GestoPreviewSurface
import it.frumorn.gesto.ui.theme.GestoTheme

/**
 * One paired PC: whether it is on, its name, what it can do, and what to do with it. The PC of the
 * session and a PC that answers are edged in primary. Rename and Forget sit behind the "⋯" menu;
 * the screen shows their dialogs.
 */
@Composable
fun PcCard(
    state: PcCardState,
    onConnect: () -> Unit,
    onResume: () -> Unit,
    onDisconnect: () -> Unit,
    onWake: () -> Unit,
    onRename: () -> Unit,
    onForget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GestoTheme.spacing
    val shape = MaterialTheme.shapes.extraSmall
    val highlighted = state.connected || state.presence == Presence.Online
    val edge by animateColorAsState(
        if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        tween(GestoMotion.ENTER_MS, easing = GestoMotion.Enter),
        label = "pc-card-edge",
    )
    Column(
        modifier
            .fillMaxWidth()
            .border(spacing.borderWidth, edge, shape)
            .background(MaterialTheme.colorScheme.background, shape)
            .padding(spacing.gutter)
            .animateContentSize(tween(GestoMotion.ENTER_MS, easing = GestoMotion.Enter)),
        verticalArrangement = Arrangement.spacedBy(spacing.xl),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                StatusLine(state)
                Spacer(Modifier.height(spacing.sm))
                Text(
                    state.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            CardMenu(name = state.name, onRename = onRename, onForget = onForget)
        }
        if (state.chips.isNotEmpty()) Chips(state.chips)
        CardActions(state, onConnect, onResume, onDisconnect, onWake)
    }
}

@Composable
private fun StatusLine(state: PcCardState) {
    val spacing = GestoTheme.spacing
    val success = GestoTheme.colors.success
    val idle = MaterialTheme.colorScheme.outline
    AnimatedContent(
        targetState = statusOf(state),
        transitionSpec = {
            fadeIn(tween(GestoMotion.ENTER_MS, easing = GestoMotion.Enter)) togetherWith
                fadeOut(tween(GestoMotion.EXIT_MS, easing = GestoMotion.Exit))
        },
        label = "pc-status",
    ) { status ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(spacing.statusDotSize)
                    .background(if (status.on) success else idle),
            )
            Spacer(Modifier.width(spacing.sm))
            Text(
                stringResource(status.text),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class Status(val text: Int, val on: Boolean)

private fun statusOf(state: PcCardState): Status = when {
    state.connected -> Status(R.string.session_connected, on = true)
    state.presence == Presence.Online -> Status(R.string.home_state_online, on = true)
    state.presence == Presence.Offline -> Status(R.string.home_state_offline, on = false)
    else -> Status(R.string.home_state_looking, on = false)
}

@Composable
private fun CardMenu(name: String, onRename: () -> Unit, onForget: () -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val spacing = GestoTheme.spacing
    Box {
        GestoIconButton(onClick = { open = true }) {
            Icon(
                GestoIcons.More,
                contentDescription = stringResource(R.string.home_more, name),
                modifier = Modifier.size(spacing.barIconSize),
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = MaterialTheme.shapes.extraSmall,
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = spacing.none,
            shadowElevation = spacing.md,
            border = BorderStroke(spacing.borderWidth, MaterialTheme.colorScheme.outlineVariant),
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_rename), style = MaterialTheme.typography.labelLarge) },
                onClick = {
                    open = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.home_forget),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = {
                    open = false
                    onForget()
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(chips: List<UiText>) {
    val spacing = GestoTheme.spacing
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        chips.forEach { chip ->
            Text(
                chip.asString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = spacing.md, vertical = spacing.xs),
            )
        }
    }
}

@Composable
private fun CardActions(
    state: PcCardState,
    onConnect: () -> Unit,
    onResume: () -> Unit,
    onDisconnect: () -> Unit,
    onWake: () -> Unit,
) {
    val spacing = GestoTheme.spacing
    val full = Modifier.fillMaxWidth()
    when {
        state.connected -> Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            GestoButton(stringResource(R.string.home_resume), onResume, full, icon = GestoIcons.Touchpad)
            GestoOutlinedButton(stringResource(R.string.quick_disconnect), onDisconnect, full)
        }
        state.presence == Presence.Offline -> if (state.canWake) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                GestoOutlinedButton(stringResource(R.string.home_wake), onWake, Modifier.weight(1f), icon = GestoIcons.Wake)
                GestoOutlinedButton(stringResource(R.string.home_connect), onConnect, Modifier.weight(1f))
            }
        } else {
            // It may still answer at an address the network scan does not reach (a VPN).
            GestoOutlinedButton(stringResource(R.string.home_connect), onConnect, full)
        }
        else -> GestoButton(stringResource(R.string.home_connect), onConnect, full, icon = GestoIcons.Touchpad)
    }
}

private val previewChips = listOf(
    UiText.Res(R.string.home_chip_screen),
    UiText.Res(R.string.home_chip_audio),
    UiText.Res(R.string.home_chip_controller),
)

@Preview(name = "PC card - states - light", heightDp = 900)
@Composable
private fun PcCardPreviewLight() = GestoPreviewSurface(darkTheme = false) { PcCardPreviewBody() }

@Preview(name = "PC card - states - dark", heightDp = 900)
@Composable
private fun PcCardPreviewDark() = GestoPreviewSurface(darkTheme = true) { PcCardPreviewBody() }

@Composable
private fun PcCardPreviewBody() {
    Column(verticalArrangement = Arrangement.spacedBy(GestoTheme.spacing.md)) {
        listOf(
            PcCardState("1", "Legion", Presence.Online, connected = true, canWake = false, chips = previewChips),
            PcCardState("2", "Desk PC", Presence.Online, connected = false, canWake = false, chips = previewChips.take(1)),
            PcCardState("3", "Living room", Presence.Offline, connected = false, canWake = true, chips = emptyList()),
            PcCardState("4", "Laptop", Presence.Unknown, connected = false, canWake = false, chips = emptyList()),
        ).forEach { PcCard(it, {}, {}, {}, {}, {}, {}) }
    }
}
