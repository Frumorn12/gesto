package it.frumorn.gesto.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import it.frumorn.gesto.R
import it.frumorn.gesto.core.model.MAX_PC_NAME_LENGTH
import it.frumorn.gesto.core.session.Presence
import it.frumorn.gesto.ui.components.GestoButton
import it.frumorn.gesto.ui.components.GestoDialog
import it.frumorn.gesto.ui.components.GestoIcons
import it.frumorn.gesto.ui.components.GestoMark
import it.frumorn.gesto.ui.components.GestoOutlinedButton
import it.frumorn.gesto.ui.components.PcCard
import it.frumorn.gesto.ui.state.HomeActions
import it.frumorn.gesto.ui.state.HomeUiState
import it.frumorn.gesto.ui.state.PcCardState
import it.frumorn.gesto.ui.text.UiText
import it.frumorn.gesto.ui.theme.GestoMotion
import it.frumorn.gesto.ui.theme.GestoTheme
import it.frumorn.gesto.ui.theme.ThemeMode

/**
 * "Your PCs": the paired PCs with their state, and "Add a PC" always at the bottom, under the thumb.
 * With no PC yet it shows the Gesto mark and one sentence instead of the list.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    val spacing = GestoTheme.spacing
    // Ids, so a dialog survives rotation and closes by itself if its PC goes away.
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    var forgetting by rememberSaveable { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxSize()) {
        if (state.pcs.isEmpty()) {
            EmptyHome(Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                item(key = "title") {
                    Text(
                        stringResource(R.string.nav_your_pcs),
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(top = spacing.sm, bottom = spacing.sm),
                    )
                }
                items(state.pcs, key = { it.id }) { pc ->
                    PcCard(
                        state = pc,
                        onConnect = { actions.onConnect(pc.id) },
                        onResume = actions.onResume,
                        onDisconnect = actions.onDisconnect,
                        onWake = { actions.onWake(pc.id) },
                        onRepair = { actions.onRepair(pc.id) },
                        onRename = { renaming = pc.id },
                        onForget = { forgetting = pc.id },
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(GestoMotion.ENTER_MS, easing = GestoMotion.Enter),
                            placementSpec = tween(GestoMotion.ENTER_MS, easing = GestoMotion.Enter),
                            fadeOutSpec = tween(GestoMotion.EXIT_MS, easing = GestoMotion.Exit),
                        ),
                    )
                }
                item(key = "end") { Spacer(Modifier.height(spacing.md)) }
            }
        }
        val addPc = stringResource(R.string.home_add_pc)
        // The only action of an empty list is filled; next to the cards it steps back to an outline.
        if (state.pcs.isEmpty()) {
            GestoButton(addPc, actions.onAddPc, Modifier.fillMaxWidth(), icon = GestoIcons.Plus)
        } else {
            GestoOutlinedButton(addPc, actions.onAddPc, Modifier.fillMaxWidth(), icon = GestoIcons.Plus)
        }
        Spacer(Modifier.height(spacing.gutter))
    }

    state.pcs.find { it.id == renaming }?.let { pc ->
        RenameDialog(
            current = pc.name,
            onDismiss = { renaming = null },
            onSave = { name ->
                actions.onRename(pc.id, name)
                renaming = null
            },
        )
    }
    state.pcs.find { it.id == forgetting }?.let { pc ->
        GestoDialog(
            title = stringResource(R.string.home_forget_title, pc.name),
            confirmText = stringResource(R.string.home_forget),
            onConfirm = {
                actions.onForget(pc.id)
                forgetting = null
            },
            dismissText = stringResource(R.string.action_cancel),
            onDismiss = { forgetting = null },
            destructive = true,
        ) {
            Text(
                stringResource(R.string.home_forget_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyHome(modifier: Modifier = Modifier) {
    val spacing = GestoTheme.spacing
    Column(
        modifier.fillMaxWidth().padding(horizontal = spacing.hero),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        GestoMark(size = spacing.emptyMarkSize)
        Spacer(Modifier.height(spacing.gutter))
        Text(
            stringResource(R.string.home_empty),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RenameDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    // The whole name starts selected: typing replaces it, a tap places the cursor.
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(current, selection = TextRange(0, current.length)))
    }
    val focus = remember { FocusRequester() }
    GestoDialog(
        title = stringResource(R.string.home_rename_title),
        confirmText = stringResource(R.string.action_save),
        onConfirm = { onSave(value.text) },
        dismissText = stringResource(R.string.action_cancel),
        onDismiss = onDismiss,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.text.length <= MAX_PC_NAME_LENGTH) value = it },
            singleLine = true,
            label = { Text(stringResource(R.string.home_rename_label)) },
            supportingText = { Text(stringResource(R.string.home_rename_hint)) },
            shape = MaterialTheme.shapes.extraSmall,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSave(value.text) }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus),
        )
        LaunchedEffect(Unit) { focus.requestFocus() }
    }
}

private val previewPcs = listOf(
    PcCardState(
        id = "1",
        name = "Legion",
        presence = Presence.Online,
        connected = false,
        canWake = false,
        chips = listOf(UiText.Res(R.string.home_chip_screen)),
    ),
    PcCardState(
        id = "2",
        name = "Living room",
        presence = Presence.Offline,
        connected = false,
        canWake = true,
        chips = emptyList(),
    ),
)

@Preview(name = "Home - light", widthDp = 412, heightDp = 860)
@Composable
private fun HomeScreenPreviewLight() = GestoTheme(mode = ThemeMode.Light) {
    Surface(color = MaterialTheme.colorScheme.background) {
        HomeScreen(HomeUiState(previewPcs), HomeActions(), Modifier.padding(GestoTheme.spacing.gutter))
    }
}

@Preview(name = "Home - empty - dark", widthDp = 412, heightDp = 860)
@Composable
private fun HomeScreenPreviewEmpty() = GestoTheme(mode = ThemeMode.Dark) {
    Surface(color = MaterialTheme.colorScheme.background) {
        HomeScreen(HomeUiState(), HomeActions(), Modifier.padding(GestoTheme.spacing.gutter))
    }
}
