package com.mgsamirr.moviebox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val AppBackground = Color(0xFF090D18)
private val SurfaceColor = Color(0xFF151C2C)
private val Accent = Color(0xFFFFC857)
private val Muted = Color(0xFF9AA6BC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    NavigationBar(containerColor = SurfaceColor) {
                        listOf("Home", "Search", "Downloads", "Settings").forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Text(tab.take(1), fontWeight = FontWeight.Bold) },
                                label = { Text(tab) }
                            )
                        }
                    }
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column {
                            Text("MOVIEBOX", color = Accent, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(6.dp))
                            Text("Movies worth watching.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Discover movies, series and anime with Hindi audio.", color = Muted)
                        }
                    }
                    item {
                        TextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Search titles") },
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = SurfaceColor,
                                unfocusedContainerColor = SurfaceColor,
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
                                    label = { Text(category.label) }
                                )
                            }
                        }
                    }
                    item {
                        Text(
                            if (selectedTab == "Downloads") "Downloads"
                            else if (selectedTab == "Settings") "Settings"
                            else "Hindi audio picks",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (visibleTitles.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(20.dp)) {
                                    Text("Your Hindi audio catalog will appear here", fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "A title is shown only after a provider confirms Hindi audio is available. The content provider is not connected yet.",
                                        color = Muted,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    } else {
                        items(visibleTitles) { title ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(title.name, fontWeight = FontWeight.Bold)
                                    Text("Hindi audio", color = Accent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
