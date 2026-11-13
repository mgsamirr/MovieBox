package com.mgsamirr.moviebox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AppBackground = Color(0xFF000000)
private val SurfaceColor = Color(0xFF101010)
private val ElevatedSurface = Color(0xFF171717)
private val Accent = Color(0xFFE8C477)
private val Muted = Color(0xFF929292)
private val Hairline = Color(0xFF292929)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        setContent { MovieBoxApp() }
    }
}

private enum class CatalogCategory(val label: String) {
    MOVIES("Movies"),
    SERIES("Web Series"),
    ANIME("Anime")
}

private data class AudioTrack(val languageCode: String, val available: Boolean)
private data class CatalogTitle(
    val id: String,
    val name: String,
    val category: CatalogCategory,
    val audioTracks: List<AudioTrack>
)

private fun hasHindiAudio(title: CatalogTitle): Boolean =
    title.audioTracks.any {
        it.available && it.languageCode.trim().lowercase() in setOf("hi", "hin", "hindi")
    }

// No sample titles are shown as real content. Connect an authorized catalog provider here.
private val catalog: List<CatalogTitle> = emptyList()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MovieBoxApp() {
    var selectedCategory by remember { mutableStateOf(CatalogCategory.MOVIES) }
    var search by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("Home") }
    val visibleTitles = catalog
        .filter(::hasHindiAudio)
        .filter { it.category == selectedCategory }
        .filter { it.name.contains(search, ignoreCase = true) }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = AppBackground, contentColor = Color.White) {
            Scaffold(
                containerColor = AppBackground,
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFF080808), contentColor = Muted) {
                        listOf("Home", "Search", "Downloads", "Settings").forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Text(
                                        when (tab) {
                                            "Home" -> "⌂"
                                            "Search" -> "⌕"
                                            "Downloads" -> "↓"
                                            else -> "⚙"
                                        },
                                        color = if (selectedTab == tab) Accent else Muted,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                },
                                label = {
                                    Text(
                                        tab,
                                        color = if (selectedTab == tab) Accent else Muted,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "SHERIF",
                                    color = Accent,
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.labelLarge,
                                    letterSpacing = 3.sp
                                )
                                Text(
                                    "MOVIE BOX",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleLarge,
                                    letterSpacing = 1.5.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .border(1.dp, Accent.copy(alpha = 0.55f), CircleShape)
                                    .background(SurfaceColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("S", color = Accent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "YOUR PRIVATE CINEMA",
                                color = Accent,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Text(
                                "Stories that\nstay with you.",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = MaterialTheme.typography.headlineLarge.lineHeight
                            )
                            Text(
                                "Discover movies, series and anime with Hindi audio.",
                                color = Muted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    item {
                        TextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Search movies, series, anime", color = Muted) },
                            leadingIcon = { Text("⌕", color = Accent, style = MaterialTheme.typography.titleLarge) },
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = ElevatedSurface,
                                unfocusedContainerColor = ElevatedSurface,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Accent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CatalogCategory.entries.forEach { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    label = { Text(category.label) },
                                    shape = RoundedCornerShape(50),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Accent,
                                        selectedLabelColor = Color.Black,
                                        labelColor = Muted,
                                        containerColor = SurfaceColor
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selectedCategory == category,
                                        borderColor = Hairline,
                                        selectedBorderColor = Accent
                                    )
                                )
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                when (selectedTab) {
                                    "Downloads" -> "Your downloads"
                                    "Settings" -> "Preferences"
                                    else -> "Made for your screen"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "HINDI AUDIO",
                                color = Accent,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (visibleTitles.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Hairline)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF19160F), SurfaceColor, SurfaceColor)
                                            )
                                        )
                                        .padding(22.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(Accent.copy(alpha = 0.12f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("✦", color = Accent, style = MaterialTheme.typography.headlineSmall)
                                        }
                                        Text(
                                            "Your next favourite\nstarts here.",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Your Hindi-audio collection will appear here once an authorized content provider is connected. We only list titles with confirmed Hindi audio.",
                                            color = Muted,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            "CURATED FOR YOU  •  HINDI AUDIO",
                                            color = Accent,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(visibleTitles, key = { it.id }) { title ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Hairline)
                            ) {
                                Column(Modifier.padding(18.dp)) {
                                    Text(title.name, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(6.dp))
                                    Text("HINDI AUDIO", color = Accent, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
