package com.mgsamirr.moviebox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun CatalogDetailScreen(
    title: CatalogTitle,
    onBack: () -> Unit,
    onToggleWatchlist: (() -> Unit)? = null,
    isSaved: Boolean = false
) {
    val accent = Color(0xFFE8C477)
    val muted = Color(0xFF929292)
    val surface = Color(0xFF101010)
    val eligible = HindiAudioPolicy.hasVerifiedHindiAudio(title)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text("← BACK", color = accent) }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = surface),
                border = BorderStroke(1.dp, Color(0xFF292929))
            ) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    title.safePosterUrl?.let { posterUrl ->
                        AsyncImage(
                            model = posterUrl,
                            contentDescription = title.name,
                            modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(title.category.label.uppercase(), color = accent, style = MaterialTheme.typography.labelMedium)
                    Text(title.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    val details = listOfNotNull(
                        title.validReleaseYear?.toString(),
                        title.validDurationMinutes?.let { "$it min" }
                    ).joinToString("  •  ")
                    if (details.isNotBlank()) Text(details, color = muted)
                    Surface(color = Color(0xFF1B1913), shape = RoundedCornerShape(50)) {
                        Text(
                            if (eligible) "HINDI AUDIO VERIFIED" else "PLAYBACK UNAVAILABLE",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = accent,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (title.safeDescription.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("About this title", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(title.safeDescription, color = muted, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (onToggleWatchlist != null && eligible) {
                        TextButton(onClick = onToggleWatchlist) {
                            Text(if (isSaved) "REMOVE FROM WATCHLIST" else "＋ ADD TO WATCHLIST", color = accent)
                        }
                    }
                }
            }
        }
        item {
            Text(
                "Playback will be enabled only when an authorized provider supplies a valid Hindi audio stream.",
                color = muted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
