package it.frumorn.gesto.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import it.frumorn.gesto.R
import it.frumorn.gesto.core.model.AUTO_LAST_RUNG
import it.frumorn.gesto.core.model.CapabilitySummary
import it.frumorn.gesto.core.model.QualityPreset
import it.frumorn.gesto.core.model.ScreenStreamStats
import it.frumorn.gesto.core.model.formatFps
import it.frumorn.gesto.ui.components.GestoButton
import it.frumorn.gesto.ui.components.GestoIcons
import it.frumorn.gesto.ui.components.SettingsDivider
import it.frumorn.gesto.ui.components.SettingsSectionLabel
import it.frumorn.gesto.ui.session.qualityLabel
import it.frumorn.gesto.ui.state.DiagnosticsActions
import it.frumorn.gesto.ui.state.DiagnosticsUiState
import it.frumorn.gesto.ui.theme.GestoPreviewSurface
import it.frumorn.gesto.ui.theme.GestoTheme

/**
 * What the PC can do and how the stream is going, grouped: connection, input, screen, audio and
 * the phone's external devices. Titles in plain words, the values as they are, in monospace.
 */
@Composable
fun DiagnosticsScreen(
    state: DiagnosticsUiState,
    actions: DiagnosticsActions,
    modifier: Modifier = Modifier,
) {
    val capabilities = state.capabilities
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = GestoTheme.spacing.gutter),
    ) {
        GestoButton(
            text = stringResource(R.string.diag_refresh),
            onClick = actions.onRefreshCapabilities,
            icon = GestoIcons.Refresh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = GestoTheme.spacing.md),
        )

        SettingsSectionLabel(stringResource(R.string.diag_section_link))
        Line(R.string.diag_state, state.connectionLabel)
        Line(R.string.diag_route, capabilities.routeBackend)
        Line(R.string.diag_home_network, yesNo(capabilities.lanDirectSupported))
        Line(R.string.diag_internet, yesNo(capabilities.publicDirectSupported))
        Line(R.string.diag_relay, yesNo(capabilities.relaySupported))
        Line(R.string.diag_details, capabilities.connectivityReason)

        SettingsSectionLabel(stringResource(R.string.diag_section_input))
        Line(R.string.diag_input_method, capabilities.inputBackend)
        Line(R.string.diag_details, capabilities.inputReason)
        Line(R.string.diag_brightness, yesNo(capabilities.brightness))
        Line(R.string.diag_clipboard, yesNo(capabilities.clipboard))
        Line(R.string.diag_lock, yesNo(capabilities.lock))
        Line(R.string.diag_suspend, yesNo(capabilities.suspend))

        SettingsSectionLabel(stringResource(R.string.diag_section_screen))
        ScreenLines(state)

        SettingsSectionLabel(stringResource(R.string.diag_section_audio))
        Line(R.string.diag_audio_from_pc, yesNo(capabilities.audioSupported))
        Line(R.string.diag_audio_muted, yesNo(state.audioMuted))
        Line(R.string.diag_volume, yesNo(capabilities.volume))

        SettingsSectionLabel(stringResource(R.string.diag_section_devices))
        Line(R.string.diag_mouse, yesNo(capabilities.externalPointerSupported))
        Line(R.string.diag_keyboard, yesNo(capabilities.externalKeyboardSupported))
        Line(R.string.diag_controller, yesNo(capabilities.externalControllerSupported))
        Line(R.string.diag_details, state.externalInputStatus)
        if (state.externalInputDevices.isEmpty()) {
            Text(
                stringResource(R.string.diag_no_devices),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = GestoTheme.spacing.md),
            )
        } else {
            state.externalInputDevices.forEach { device -> Line(device.name, device.displayClasses) }
        }

        OnThePc(Modifier.padding(top = GestoTheme.spacing.section))
    }
}

@Composable
private fun ScreenLines(state: DiagnosticsUiState) {
    val capabilities = state.capabilities
    val stats = state.stats
    val quality = stringResource(state.quality.qualityLabel)
    Line(R.string.diag_sharing, capabilities.captureBackend)
    Line(R.string.diag_details, capabilities.captureReason)
    Line(R.string.diag_streaming, yesNo(state.screenStreaming))
    Line(
        R.string.diag_quality,
        if (state.quality == QualityPreset.Auto) {
            stringResource(R.string.diag_quality_auto, quality, state.autoQualityRung + 1, AUTO_LAST_RUNG + 1)
        } else {
            quality
        },
    )
    Line(R.string.diag_video, listOf(stats.backend, stats.codec).filter { it.isNotBlank() }.joinToString(" · "))
    Line(R.string.diag_fps_asked, "${stats.targetFps}")
    Line(R.string.diag_fps_pc, "${stats.actualFps}")
    Line(R.string.diag_fps_shown, stats.deliveredFps.formatFps())
    Line(R.string.diag_frame_size, "${stats.averageKib} KiB")
    Line(R.string.diag_bitrate, "${stats.kilobitsPerSecond} kbps")
    Line(R.string.diag_frames, "${stats.receivedFrames}")
    Line(R.string.diag_status, state.screenStatus)
    Line(R.string.diag_last_error, state.screenError ?: stringResource(R.string.diag_none))
}

@Composable
private fun yesNo(value: Boolean): String = stringResource(if (value) R.string.diag_yes else R.string.diag_no)

@Composable
private fun Line(@StringRes label: Int, value: String) = Line(stringResource(label), value)

/** `label ........ value`, the value in monospace, a line under it. */
@Composable
private fun Line(label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = GestoTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(GestoTheme.spacing.xl),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(LABEL_WEIGHT),
            )
            Text(
                value,
                style = GestoTheme.accentTypography.readout,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(VALUE_WEIGHT),
            )
        }
        SettingsDivider()
    }
}

private const val LABEL_WEIGHT = 0.42f
private const val VALUE_WEIGHT = 0.58f

/** "On the PC: `gesto doctor`", the command in monospace. */
@Composable
private fun OnThePc(modifier: Modifier = Modifier) {
    val command = stringResource(R.string.diag_doctor_command)
    val line = stringResource(R.string.diag_on_pc, command)
    val start = line.indexOf(command)
    val mono = SpanStyle(
        fontFamily = GestoTheme.accentTypography.command.fontFamily,
        fontWeight = GestoTheme.accentTypography.command.fontWeight,
    )
    Text(
        buildAnnotatedString {
            if (start < 0) {
                append(line)
            } else {
                append(line.substring(0, start))
                withStyle(mono) { append(command) }
                append(line.substring(start + command.length))
            }
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Preview(name = "Diagnostics - dark", heightDp = 1800)
@Composable
private fun DiagnosticsScreenPreviewDark() = GestoPreviewSurface(darkTheme = true) {
    DiagnosticsScreen(
        DiagnosticsUiState(
            connectionLabel = "Connected",
            capabilities = CapabilitySummary(inputSupported = true, inputBackend = "uinput", captureSupported = true, captureBackend = "wayland-screencast-portal"),
            screenStreaming = true,
            stats = ScreenStreamStats(deliveredFps = 58.0, targetFps = 60, actualFps = 60, backend = "wayland-screencast-portal", codec = "h264"),
            autoQualityRung = 1,
        ),
        DiagnosticsActions(),
    )
}

@Preview(name = "Diagnostics - light", heightDp = 1800)
@Composable
private fun DiagnosticsScreenPreviewLight() = GestoPreviewSurface(darkTheme = false) {
    DiagnosticsScreen(DiagnosticsUiState(), DiagnosticsActions())
}
