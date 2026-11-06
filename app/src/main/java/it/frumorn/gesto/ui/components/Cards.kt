package it.frumorn.gesto.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import it.frumorn.gesto.ui.theme.GestoPreviewSurface
import it.frumorn.gesto.ui.theme.GestoTheme

/** The one card style used across the app: tonal container, hairline outline, extra-large corner. */
@Composable
fun GestoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.extraLarge
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    val border = BorderStroke(GestoTheme.spacing.borderWidth, MaterialTheme.colorScheme.outlineVariant)
    val inner: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(GestoTheme.spacing.gutter), content = content)
    }
    if (onClick == null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            border = border,
            content = inner,
        )
    } else {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            border = border,
            content = inner,
        )
    }
}

@Preview(name = "Cards - dark")
@Composable
private fun GestoCardsPreviewDark() = GestoPreviewSurface(darkTheme = true) { CardsPreviewBody() }

@Preview(name = "Cards - light")
@Composable
private fun GestoCardsPreviewLight() = GestoPreviewSurface(darkTheme = false) { CardsPreviewBody() }

@Composable
private fun CardsPreviewBody() {
    Column(verticalArrangement = Arrangement.spacedBy(GestoTheme.spacing.xl)) {
        GestoCard {
            Text("Card", style = MaterialTheme.typography.titleMedium)
        }
        GestoCard(onClick = {}) {
            Text("Tappable card", style = MaterialTheme.typography.titleMedium)
        }
    }
}
