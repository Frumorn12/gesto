package it.frumorn.gesto.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.frumorn.gesto.AddPcStep
import it.frumorn.gesto.R
import it.frumorn.gesto.ui.components.GestoButton
import it.frumorn.gesto.ui.components.GestoIcons
import it.frumorn.gesto.ui.components.GestoLoadingBar
import it.frumorn.gesto.ui.components.GestoTextButton
import it.frumorn.gesto.ui.state.AddPcActions
import it.frumorn.gesto.ui.state.AddPcUiState
import it.frumorn.gesto.ui.state.FoundPcState
import it.frumorn.gesto.ui.theme.GestoMotion
import it.frumorn.gesto.ui.theme.GestoTheme
import it.frumorn.gesto.ui.theme.ThemeMode

/** The two parts of adding a PC: finding it, then letting the phone in on it. */
private const val STEP_COUNT = 2

/** Squares of the search animation, and how long one takes to grow and fade. */
private const val RADAR_RINGS = 3
private const val RADAR_MS = 2200
private const val RADAR_START_SCALE = 0.2f
private const val RADAR_START_ALPHA = 0.9f

/** The page a step shows; the camera opens over the search. */
private enum class AddPcPage { Search, Approval, Code, Address, Invite }

private val AddPcStep.page: AddPcPage
    get() = when (this) {
        AddPcStep.Search, AddPcStep.Qr -> AddPcPage.Search
        AddPcStep.Approval -> AddPcPage.Approval
        AddPcStep.Code -> AddPcPage.Code
        AddPcStep.Address -> AddPcPage.Address
        AddPcStep.Invite -> AddPcPage.Invite
    }

/** Title of the page bar for [step]. */
fun addPcTitle(step: AddPcStep): Int = when (step.page) {
    AddPcPage.Approval, AddPcPage.Code -> R.string.add_confirm_title
    AddPcPage.Search, AddPcPage.Address, AddPcPage.Invite -> R.string.home_add_pc
}

/**
 * "Add a PC". Search: the network is scanned while the squares grow, found PCs appear as rows, the
 * QR and the address are below for a PC that does not show up. Approval: the PIN the PC shows in its
 * notification, and a wait for the person there to press Allow. Code and Address are the fallbacks.
 */
@Composable
fun AddPcScreen(
    state: AddPcUiState,
    actions: AddPcActions,
    modifier: Modifier = Modifier,
) {
    val enterOffset = with(LocalDensity.current) { GestoMotion.ENTER_OFFSET_DP.dp.roundToPx() }
    AnimatedContent(
        targetState = state.step.page,
        transitionSpec = { GestoMotion.screenChange(enterOffset) },
        modifier = modifier.fillMaxSize(),
        label = "add-pc-step",
    ) { page ->
        when (page) {
            AddPcPage.Search -> SearchStep(state.found, actions, enterOffset)
            AddPcPage.Approval -> ApprovalStep(state.pin, actions.onCancelApproval)
            AddPcPage.Code -> CodeStep(state, actions)
            AddPcPage.Address -> AddressStep(state, actions)
            AddPcPage.Invite -> InviteStep(state, actions)
        }
    }
}

@Composable
private fun SearchStep(found: List<FoundPcState>, actions: AddPcActions, enterOffset: Int) {
    val spacing = GestoTheme.spacing
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StepBars(current = 1)
            Spacer(Modifier.height(spacing.md))
            Radar()
            Spacer(Modifier.height(spacing.xs))
            Text(
                stringResource(R.string.add_searching),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.add_searching_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(spacing.gutter))
            found.forEach { pc ->
                key(pc.key) {
                    FoundPcRow(pc, onClick = { actions.onSelectPc(pc.key) }, enterOffset = enterOffset)
                    Spacer(Modifier.height(spacing.md))
                }
            }
        }
        Spacer(Modifier.height(spacing.md))
        Text(
            stringResource(R.string.add_cant_find).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        GestoTextButton(
            text = stringResource(R.string.add_scan_qr),
            onClick = actions.onOpenQr,
            modifier = Modifier.fillMaxWidth(),
            icon = GestoIcons.Qr,
        )
        GestoTextButton(
            text = stringResource(R.string.add_type_address),
            onClick = actions.onOpenAddress,
            modifier = Modifier.fillMaxWidth(),
            icon = GestoIcons.Edit,
        )
        Spacer(Modifier.height(spacing.gutter))
    }
}

/** Squares that grow from the centre and fade, one after the other, while the network is scanned. */
@Composable
private fun Radar(modifier: Modifier = Modifier) {
    val spacing = GestoTheme.spacing
    val phase by rememberInfiniteTransition(label = "radar").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(RADAR_MS, easing = LinearEasing)),
        label = "radar-phase",
    )
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier.size(spacing.radarSize)) {
        val stroke = Stroke(spacing.borderWidth.toPx())
        repeat(RADAR_RINGS) { ring ->
            val t = GestoMotion.Enter.transform((phase + ring.toFloat() / RADAR_RINGS) % 1f)
            val side = size.minDimension * (RADAR_START_SCALE + (1f - RADAR_START_SCALE) * t)
            drawRect(
                color = color,
                topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f),
                size = Size(side, side),
                alpha = RADAR_START_ALPHA * (1f - t),
                style = stroke,
            )
        }
        val core = spacing.radarCoreSize.toPx()
        drawRect(color, topLeft = center - Offset(core / 2f, core / 2f), size = Size(core, core))
    }
}

/** A PC the search found; it rises in when it first answers. */
@Composable
private fun FoundPcRow(pc: FoundPcState, onClick: () -> Unit, enterOffset: Int) {
    val spacing = GestoTheme.spacing
    val shape = MaterialTheme.shapes.extraSmall
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = shown,
        enter = fadeIn(tween(GestoMotion.ENTER_MS, easing = GestoMotion.Enter)) +
            slideInVertically(tween(GestoMotion.ENTER_MS, easing = GestoMotion.Enter)) { enterOffset },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .border(spacing.borderWidth, MaterialTheme.colorScheme.primary, shape)
                .background(MaterialTheme.colorScheme.background, shape)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(spacing.xxl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(spacing.foundIconBox)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    GestoIcons.Pc,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(spacing.barIconSize),
                )
            }
            Spacer(Modifier.width(spacing.xl))
            Column(Modifier.weight(1f)) {
                Text(
                    pc.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(if (pc.alreadyAdded) R.string.add_found_added else R.string.add_found_new),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                GestoIcons.Chevron,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(spacing.menuIconSize),
            )
        }
    }
}

@Composable
private fun ApprovalStep(pin: String, onCancel: () -> Unit) {
    val spacing = GestoTheme.spacing
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        StepBars(current = 2)
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(R.string.add_pin_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.lg))
        PinBoxes(pin)
        Spacer(Modifier.height(spacing.lg))
        AllowLine()
        Spacer(Modifier.height(spacing.section))
        GestoLoadingBar()
        Spacer(Modifier.height(spacing.md))
        Text(
            stringResource(R.string.add_pin_waiting),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        GestoTextButton(
            text = stringResource(R.string.action_cancel),
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(spacing.gutter))
    }
}

/** The four digits, one per box, read out one by one by TalkBack. */
@Composable
private fun PinBoxes(pin: String) {
    val spacing = GestoTheme.spacing
    val description = stringResource(R.string.add_pin_described, pin.toList().joinToString(" "))
    Row(
        Modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        pin.forEach { digit ->
            Box(
                Modifier
                    .size(width = spacing.pinBoxWidth, height = spacing.pinBoxHeight)
                    .border(spacing.borderWidth, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall),
                contentAlignment = Alignment.Center,
            ) {
                Text(digit.toString(), style = GestoTheme.accentTypography.pinDigit)
            }
        }
    }
}

/** "If it's the same, press Allow on the PC.", with the button's name in primary. */
@Composable
private fun AllowLine() {
    val button = stringResource(R.string.add_pin_allow_button)
    val line = stringResource(R.string.add_pin_allow, button)
    val primary = MaterialTheme.colorScheme.primary
    val text = remember(line, button, primary) {
        buildAnnotatedString {
            val at = line.indexOf(button)
            if (at < 0) {
                append(line)
            } else {
                append(line.substring(0, at))
                withStyle(SpanStyle(color = primary)) { append(button) }
                append(line.substring(at + button.length))
            }
        }
    }
    Text(text, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
}

@Composable
private fun CodeStep(state: AddPcUiState, actions: AddPcActions) {
    val spacing = GestoTheme.spacing
    val focus = remember { FocusRequester() }
    val ready = state.code.length == 6 && !state.busy
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        StepBars(current = 2, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(spacing.gutter))
        Text(
            stringResource(R.string.add_code_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(spacing.sm))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(R.string.add_code_command),
                style = GestoTheme.accentTypography.command,
                modifier = Modifier.padding(horizontal = spacing.xl, vertical = spacing.lg),
            )
        }
        Spacer(Modifier.height(spacing.xs))
        Text(
            stringResource(R.string.add_code_old, stringResource(R.string.add_code_command_old)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(spacing.gutter))
        OutlinedTextField(
            value = state.code,
            onValueChange = actions.onCodeChange,
            label = { Text(stringResource(R.string.add_code_label)) },
            singleLine = true,
            enabled = !state.busy,
            textStyle = MaterialTheme.typography.headlineSmall,
            shape = MaterialTheme.shapes.extraSmall,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (ready) actions.onPairWithCode() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus),
        )
        Spacer(Modifier.height(spacing.xl))
        GestoButton(
            text = stringResource(R.string.add_code_submit),
            onClick = actions.onPairWithCode,
            modifier = Modifier.fillMaxWidth(),
            enabled = ready,
        )
        if (state.busy) {
            Spacer(Modifier.height(spacing.xl))
            GestoLoadingBar(Modifier.align(Alignment.CenterHorizontally))
        }
        Spacer(Modifier.height(spacing.gutter))
    }
    // An invite arrives with its code already in: no keyboard for that.
    LaunchedEffect(Unit) { if (state.code.isEmpty()) focus.requestFocus() }
}

/**
 * An invite (a QR, a link) names a PC and where it is: nothing is paired until the user, who sees
 * both, presses Pair. A link can come from anywhere.
 */
@Composable
private fun InviteStep(state: AddPcUiState, actions: AddPcActions) {
    val spacing = GestoTheme.spacing
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepBars(current = 2)
        Spacer(Modifier.height(spacing.section))
        Text(
            stringResource(R.string.add_invite_question, state.inviteName, state.inviteAddress),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.md))
        Text(
            stringResource(R.string.add_invite_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.section))
        GestoButton(
            text = stringResource(R.string.action_pair),
            onClick = actions.onConfirmInvite,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.busy,
        )
        Spacer(Modifier.height(spacing.md))
        GestoTextButton(
            text = stringResource(R.string.action_cancel),
            onClick = actions.onCancelInvite,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.busy) {
            Spacer(Modifier.height(spacing.xl))
            GestoLoadingBar()
        }
        Spacer(Modifier.height(spacing.gutter))
    }
}

@Composable
private fun AddressStep(state: AddPcUiState, actions: AddPcActions) {
    val spacing = GestoTheme.spacing
    val focus = remember { FocusRequester() }
    val ready = state.address.isNotBlank() && !state.busy
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        StepBars(current = 1, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(spacing.gutter))
        Text(
            stringResource(R.string.add_address_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(spacing.xl))
        OutlinedTextField(
            value = state.address,
            onValueChange = actions.onAddressChange,
            label = { Text(stringResource(R.string.add_address_label)) },
            singleLine = true,
            shape = MaterialTheme.shapes.extraSmall,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus),
        )
        Spacer(Modifier.height(spacing.md))
        OutlinedTextField(
            value = state.port,
            onValueChange = actions.onPortChange,
            label = { Text(stringResource(R.string.add_port_label)) },
            singleLine = true,
            shape = MaterialTheme.shapes.extraSmall,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (ready) actions.onUseAddress() }),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(spacing.xl))
        GestoButton(
            text = stringResource(R.string.add_address_submit),
            onClick = actions.onUseAddress,
            modifier = Modifier.fillMaxWidth(),
            enabled = ready,
        )
        Spacer(Modifier.height(spacing.gutter))
    }
    LaunchedEffect(Unit) { if (state.address.isEmpty()) focus.requestFocus() }
}

/** Where the user is in adding a PC: one bar per part, lit up to the [current] one. */
@Composable
private fun StepBars(current: Int, modifier: Modifier = Modifier) {
    val spacing = GestoTheme.spacing
    val description = stringResource(R.string.add_steps, current, STEP_COUNT)
    Row(
        modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        repeat(STEP_COUNT) { index ->
            Box(
                Modifier
                    .size(width = spacing.stepBarWidth, height = spacing.stepBarHeight)
                    .background(
                        if (index < current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

private val previewFound = listOf(
    FoundPcState(key = "fp-legion", name = "Legion", alreadyAdded = false),
    FoundPcState(key = "fp-desk", name = "Desk PC", alreadyAdded = true),
)

@Composable
private fun AddPcPreview(mode: ThemeMode, state: AddPcUiState) = GestoTheme(mode = mode) {
    Surface(color = MaterialTheme.colorScheme.background) {
        AddPcScreen(state, AddPcActions(), Modifier.padding(GestoTheme.spacing.gutter))
    }
}

@Preview(name = "Add a PC - search - light", widthDp = 412, heightDp = 860)
@Composable
private fun AddPcSearchPreview() = AddPcPreview(ThemeMode.Light, AddPcUiState(found = previewFound))

@Preview(name = "Add a PC - PIN - dark", widthDp = 412, heightDp = 860)
@Composable
private fun AddPcApprovalPreview() =
    AddPcPreview(ThemeMode.Dark, AddPcUiState(step = AddPcStep.Approval, pin = "4821", busy = true))

@Preview(name = "Add a PC - code - light", widthDp = 412, heightDp = 860)
@Composable
private fun AddPcCodePreview() = AddPcPreview(ThemeMode.Light, AddPcUiState(step = AddPcStep.Code, code = "1234"))

@Preview(name = "Add a PC - invite - light", widthDp = 412, heightDp = 860)
@Composable
private fun AddPcInvitePreview() = AddPcPreview(
    ThemeMode.Light,
    AddPcUiState(step = AddPcStep.Invite, inviteName = "Legion", inviteAddress = "192.168.0.22"),
)

@Preview(name = "Add a PC - address - dark", widthDp = 412, heightDp = 860)
@Composable
private fun AddPcAddressPreview() =
    AddPcPreview(ThemeMode.Dark, AddPcUiState(step = AddPcStep.Address, address = "192.168.0.22"))
