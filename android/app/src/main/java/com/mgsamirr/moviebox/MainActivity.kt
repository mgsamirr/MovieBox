package com.mgsamirr.moviebox

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val AppBackground = Color(0xFF000000)
private val SurfaceColor = Color(0xFF101010)
private val ElevatedSurface = Color(0xFF171717)
private val Accent = Color(0xFFE8C477)
private val Muted = Color(0xFF929292)
private val Hairline = Color(0xFF292929)

private enum class AppTab(val label: String, val symbol: String) {
    HOME("Home", "⌂"),
    SEARCH("Search", "⌕"),
    DOWNLOADS("Downloads", "↓"),
    WATCHLIST("Watchlist", "♡"),
    SETTINGS("Settings", "⚙")
}

// Connect only a licensed catalog implementation. Empty by design until configured.
private val catalog: List<CatalogTitle> = emptyList()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        setContent { MovieBoxApp() }
    }
}

@Composable
private fun MovieBoxApp() {
    var selectedTab by remember { mutableStateOf(AppTab.HOME) }
    var selectedCategory by remember { mutableStateOf(CatalogCategory.MOVIES) }
    var search by remember { mutableStateOf("") }
    var openedTitle by remember { mutableStateOf<CatalogTitle?>(null) }
    var localVideoUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val localVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            localVideoUri = uri
        }
    }
    val preferences = remember(context) { context.getSharedPreferences("movie_box", android.content.Context.MODE_PRIVATE) }
    var savedIds by remember {
        mutableStateOf(preferences.getStringSet("watchlist_ids", emptySet())?.toSet() ?: emptySet())
    }
    fun toggleSaved(titleId: String) {
        val next = if (titleId in savedIds) savedIds - titleId else savedIds + titleId
        savedIds = next
        preferences.edit().putStringSet("watchlist_ids", next.toSet()).apply()
    }
    val watchlistTitles = remember(savedIds) {
        WatchlistState.fromIds(savedIds).filter(HindiAudioPolicy.filterVerifiedHindiTitles(catalog))
    }

    val visibleTitles = remember(selectedCategory, search) {
        HindiAudioPolicy.filterVerifiedHindiTitles(catalog)
            .filter { it.category == selectedCategory }
            .filter { it.name.contains(search, ignoreCase = true) }
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = AppBackground, contentColor = Color.White) {
            Scaffold(
                containerColor = AppBackground,
                bottomBar = {
                    if (openedTitle == null && localVideoUri == null) {
                    NavigationBar(containerColor = Color(0xFF080808), contentColor = Muted) {
                        AppTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Text(
                                        tab.symbol,
                                        color = if (selectedTab == tab) Accent else Muted,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                },
                                label = {
                                    Text(
                                        tab.label,
                                        color = if (selectedTab == tab) Accent else Muted,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                    }
                }
            ) { padding ->
                val localVideo = localVideoUri
                val detail = openedTitle
                if (localVideo != null) {
                    LocalPlaybackScreen(uri = localVideo, onBack = { localVideoUri = null })
                } else if (detail != null) {
                    CatalogDetailScreen(
                        title = detail,
                        onBack = { openedTitle = null },
                        onToggleWatchlist = { toggleSaved(detail.id) },
                        isSaved = detail.id in savedIds
                    )
                } else when (selectedTab) {
                    AppTab.HOME -> HomeScreen(
                        padding = padding,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        search = search,
                        onSearchChanged = { search = it },
                        titles = visibleTitles,
                        onOpenSearch = { selectedTab = AppTab.SEARCH },
                        savedIds = savedIds,
                        onToggleSaved = ::toggleSaved,
                        onOpenTitle = { openedTitle = it }
                    )
                    AppTab.SEARCH -> SearchScreen(
                        padding = padding,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        search = search,
                        onSearchChanged = { search = it },
                        titles = visibleTitles,
                        savedIds = savedIds,
                        onToggleSaved = ::toggleSaved,
                        onOpenTitle = { openedTitle = it }
                    )
                    AppTab.DOWNLOADS -> DownloadsScreen(
                        padding = padding,
                        onOpenLocalVideo = { localVideoPicker.launch(arrayOf("video/*")) }
                    )
                    AppTab.WATCHLIST -> WatchlistScreen(padding, watchlistTitles, onToggleSaved = ::toggleSaved, onOpenTitle = { openedTitle = it })
                    AppTab.SETTINGS -> SettingsScreen(padding)
                }
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "MOVIE BOX",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleLarge,
            letterSpacing = 1.5.sp
        )
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

@Composable
private fun PosterThumbnail(title: CatalogTitle) {
    val posterUrl = title.safePosterUrl
    if (posterUrl != null) {
        AsyncImage(
            model = posterUrl,
            contentDescription = title.name,
            modifier = Modifier.size(72.dp, 104.dp).clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier.size(72.dp, 104.dp).background(ElevatedSurface, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("MB", color = Accent, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
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

@Composable
private fun CategoryChips(
    selected: CatalogCategory,
    onSelected: (CatalogCategory) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CatalogCategory.entries.forEach { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelected(category) },
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
                    selected = selected == category,
                    borderColor = Hairline,
                    selectedBorderColor = Accent
                )
            )
        }
    }
}

@Composable
private fun HomeScreen(
    padding: PaddingValues,
    selectedCategory: CatalogCategory,
    onCategorySelected: (CatalogCategory) -> Unit,
    search: String,
    onSearchChanged: (String) -> Unit,
    titles: List<CatalogTitle>,
    onOpenSearch: () -> Unit,
    savedIds: Set<String>,
    onToggleSaved: (String) -> Unit,
    onOpenTitle: (CatalogTitle) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item { BrandHeader() }
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
            SearchField(value = search, onValueChange = onSearchChanged)
            Spacer(Modifier.height(10.dp))
            Text(
                "OPEN FULL SEARCH",
                modifier = Modifier.clickable(onClick = onOpenSearch).padding(vertical = 6.dp),
                color = Accent,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp
            )
        }
        item { CategoryChips(selectedCategory, onCategorySelected) }
        item { SectionHeading("Made for your screen") }
        titleItems(titles, savedIds, onToggleSaved, onOpenTitle)
    }
}

@Composable
private fun SearchScreen(
    padding: PaddingValues,
    selectedCategory: CatalogCategory,
    onCategorySelected: (CatalogCategory) -> Unit,
    search: String,
    onSearchChanged: (String) -> Unit,
    titles: List<CatalogTitle>,
    savedIds: Set<String>,
    onToggleSaved: (String) -> Unit,
    onOpenTitle: (CatalogTitle) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { BrandHeader() }
        item {
            Text("Search", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Only titles with confirmed Hindi audio are eligible.", color = Muted)
        }
        item { SearchField(search, onSearchChanged) }
        item { CategoryChips(selectedCategory, onCategorySelected) }
        titleItems(titles, savedIds, onToggleSaved, onOpenTitle)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.titleItems(
    titles: List<CatalogTitle>,
    savedIds: Set<String>,
    onToggleSaved: (String) -> Unit,
    onOpenTitle: (CatalogTitle) -> Unit
) {
    if (titles.isEmpty()) {
        item {
            EmptyCollectionCard(
                title = "No titles to show yet.",
                message = "The catalog is waiting for an authorized provider. Titles appear only after Hindi audio availability is confirmed."
            )
        }
    } else {
        items(titles, key = { it.id }) { title ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().clickable { onOpenTitle(title) },
                border = BorderStroke(1.dp, Hairline)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PosterThumbnail(title)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(title.name, fontWeight = FontWeight.Bold)
                        Text("HINDI AUDIO VERIFIED", color = Accent, style = MaterialTheme.typography.labelSmall)
                        TextButton(onClick = { onToggleSaved(title.id) }) {
                            Text(if (title.id in savedIds) "SAVED ✓" else "＋ WATCHLIST", color = Accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchlistScreen(
    padding: PaddingValues,
    titles: List<CatalogTitle>,
    onToggleSaved: (String) -> Unit,
    onOpenTitle: (CatalogTitle) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { BrandHeader() }
        item {
            Text("My Watchlist", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Your saved titles with verified Hindi audio.", color = Muted)
        }
        if (titles.isEmpty()) {
            item {
                EmptyCollectionCard(
                    "Your watchlist is empty.",
                    "Save a title using the Watchlist button. Only authorized titles with verified Hindi audio are kept here."
                )
            }
        } else {
            items(titles, key = { it.id }) { title ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onOpenTitle(title) },
                    border = BorderStroke(1.dp, Hairline)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        PosterThumbnail(title)
                        Column(Modifier.weight(1f).clickable { onOpenTitle(title) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(title.name, fontWeight = FontWeight.Bold)
                            Text("HINDI AUDIO VERIFIED", color = Accent, style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { onToggleSaved(title.id) }) {
                            Text("REMOVE", color = Muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("HINDI AUDIO", color = Accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyStateScreen(padding: PaddingValues, title: String, message: String) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item { BrandHeader() }
        item { Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { EmptyCollectionCard(title, message) }
    }
}

@Composable
private fun DownloadsScreen(
    padding: PaddingValues,
    onOpenLocalVideo: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { BrandHeader() }
        item {
            Text("Downloads", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Offline viewing", color = Muted)
        }
        item {
            EmptyCollectionCard(
                title = "No provider downloads yet.",
                message = "Offline downloads will appear here when a connected licensed provider supports them."
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Hairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Play a file from this device", fontWeight = FontWeight.Bold)
                    Text(
                        "Choose a video using Android's file picker. Movie Box requests access only to the selected file and does not enable playback unless a Hindi language track is detected.",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = onOpenLocalVideo) {
                        Text("CHOOSE LOCAL VIDEO", color = Accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(padding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { BrandHeader() }
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Your viewing preferences", color = Muted)
        }
        item {
            SettingsRow(
                title = "Hindi audio only",
                description = "Titles without a confirmed Hindi audio track stay hidden.",
                checked = true
            )
        }
        item {
            SettingsRow(
                title = "AMOLED dark theme",
                description = "Pure black background for OLED displays.",
                checked = true
            )
        }
        item {
            SettingsRow(
                title = "Live TV and IPTV",
                description = "Not included in Movie Box.",
                checked = false,
                showSwitch = false
            )
        }
        item {
            Text(
                "Content is shown only through authorized providers. Playback and offline downloads become available after a supported provider is connected.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    description: String,
    checked: Boolean,
    showSwitch: Boolean = true
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Hairline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            if (showSwitch) {
                Switch(checked = checked, onCheckedChange = null, enabled = false)
            } else {
                Text("OFF", color = Muted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun EmptyCollectionCard(title: String, message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Hairline)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF19160F), SurfaceColor, SurfaceColor)))
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(48.dp).background(Accent.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✦", color = Accent, style = MaterialTheme.typography.headlineSmall)
                }
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(message, color = Muted, style = MaterialTheme.typography.bodyMedium)
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
