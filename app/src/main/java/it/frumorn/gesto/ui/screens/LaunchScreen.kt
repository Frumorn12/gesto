package it.frumorn.gesto.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import it.frumorn.gesto.R
import it.frumorn.gesto.SessionMode
import it.frumorn.gesto.ui.components.GestoLoadingBar
import it.frumorn.gesto.ui.components.GestoTextButton
import it.frumorn.gesto.ui.components.WritingGestoMark
import it.frumorn.gesto.ui.state.LaunchActions
import it.frumorn.gesto.ui.state.LaunchUiState
import it.frumorn.gesto.ui.theme.GestoPreviewSurface
import it.frumorn.gesto.ui.theme.GestoTheme

/** Shown while the app reconnects to the last PC on opening: the g writes itself, a thin bar runs. */
@Composable
fun LaunchScreen(
    state: LaunchUiState,
    actions: LaunchActions,
    modifier: Modifier = Modifier,
    animateMark: Boolean = true,
) {
    val spacing = GestoTheme.spacing
    val mode = stringResource(
        when (state.mode) {
            SessionMode.Touchpad -> R.string.launch_mode_touchpad
            SessionMode.Display -> R.string.launch_mode_display
            SessionMode.Commands -> R.string.launch_mode_commands
        },
    )
    val subtitle = stringResource(
        if (state.sameNetwork) R.string.launch_subtitle_lan else R.string.launch_subtitle,
        mode,
    )
    Column(
        modifier
            .fillMaxSize()
            .padding(spacing.hero),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        WritingGestoMark(size = spacing.launchMarkSize, animate = animateMark)
        Spacer(Modifier.height(spacing.gutter))
        Text(
            stringResource(R.string.launch_connecting_to).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            state.hostName,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.xl))
        GestoLoadingBar()
        Spacer(Modifier.height(spacing.xl))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.hero))
        GestoTextButton(
            text = stringResource(R.string.launch_choose_another),
            onClick = actions.onChooseAnotherPc,
        )
    }
}

@Preview(name = "Launch - light", heightDp = 640)
@Composable
private fun LaunchScreenPreviewLight() = GestoPreviewSurface(darkTheme = false) {
    LaunchScreen(
        LaunchUiState(hostName = "Legion", mode = SessionMode.Touchpad, sameNetwork = true),
        LaunchActions(),
        animateMark = false,
    )
}

@Preview(name = "Launch - dark", heightDp = 640)
@Composable
private fun LaunchScreenPreviewDark() = GestoPreviewSurface(darkTheme = true) {
    LaunchScreen(
        LaunchUiState(hostName = "Legion", mode = SessionMode.Display),
        LaunchActions(),
        animateMark = false,
    )
}
