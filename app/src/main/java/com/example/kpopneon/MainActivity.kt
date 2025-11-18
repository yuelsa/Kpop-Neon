package com.example.kpopneon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KpopNeonApp()
        }
    }
}

// ---------- Data models ----------

data class KpopArtist(
    val id: String,
    val name: String,
    val country: String?,
    val disambiguation: String?,
    val type: String?
)

sealed class ArtistResult {
    data class Success(val artists: List<KpopArtist>) : ArtistResult()
    data class Error(val message: String) : ArtistResult()
}

// ---------- Composables ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KpopNeonApp() {
    // Neon pink theme
    val neonPink = Color(0xFFFF00C8)
    val deepPink = Color(0xFFE91E63)
    val darkBackground = Color(0xFF0066FF)

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = neonPink,
            secondary = deepPink,
            background = darkBackground,
            surface = Color(0xFF120424),
            onPrimary = Color.Black,
            onSecondary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        val scope = rememberCoroutineScope()

        var query by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var artists by remember { mutableStateOf<List<KpopArtist>>(emptyList()) }

        var showInfoSheet by remember { mutableStateOf(false) }

        if (showInfoSheet) {
            ModalBottomSheet(
                onDismissRequest = { showInfoSheet = false }
            ) {
                InfoSheetContent()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "K-Pop Neon Finder",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        titleContentColor = neonPink
                    ),
                    actions = {
                        IconButton(onClick = { showInfoSheet = true }) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "About this app",
                                tint = neonPink
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0077FF),
                                Color(0xFF33DDFF)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Search K-Pop artists using MusicBrainz.",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = Color(0xFFFFB8F0)
                    )

                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Artist or group name") },
                        singleLine = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search icon"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = neonPink,
                            unfocusedBorderColor = deepPink,
                            cursorColor = neonPink,
                            focusedLabelColor = neonPink,
                            unfocusedLabelColor = Color(0xFFFFB8F0)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (query.isBlank()) {
                                errorMessage = "Please type an artist name."
                                artists = emptyList()
                                return@Button
                            }

                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                artists = emptyList()

                                val result = searchKpopArtists(query)

                                when (result) {
                                    is ArtistResult.Success -> {
                                        artists = result.artists
                                        if (artists.isEmpty()) {
                                            errorMessage = "No K-Pop artists found for this search."
                                        }
                                    }
                                    is ArtistResult.Error -> {
                                        errorMessage = result.message
                                    }
                                }

                                isLoading = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Search", fontWeight = FontWeight.Bold)
                    }

                    if (isLoading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(color = neonPink)
                    }

                    errorMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = msg,
                            color = Color(0xFFFF8A80),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (artists.isNotEmpty()) {
                        Text(
                            text = "Results",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = neonPink,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(bottom = 8.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(artists) { artist ->
                                ArtistCard(artist = artist)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistCard(artist: KpopArtist) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0033FF)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = artist.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFEBFE)
            )

            artist.type?.let {
                if (it.isNotBlank()) {
                    Text(
                        text = "Type: $it",
                        fontSize = 13.sp,
                        color = Color(0xFFEF9A9A),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            artist.country?.let {
                if (it.isNotBlank()) {
                    Text(
                        text = "Country: $it",
                        fontSize = 13.sp,
                        color = Color(0xFFB39DDB),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            artist.disambiguation?.let {
                if (it.isNotBlank()) {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = Color(0xFFFFCDD2),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InfoSheetContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "About K-Pop Neon Finder",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "This app uses the MusicBrainz API to search for artists tagged as K-Pop.",
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Type the name of a K-Pop group or artist (for example: BTS, Stray Kids), " +
                    "then tap Search to see matching artists.",
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Data source: musicbrainz.org. This is a community-maintained open music database.",
            fontSize = 13.sp,
            color = Color(0xFFBDBDBD)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ---------- Network layer ----------

suspend fun searchKpopArtists(query: String): ArtistResult {
    return withContext(Dispatchers.IO) {
        try {
            // Encode query for URL
            val encodedName = URLEncoder.encode(query, "UTF-8")
            val mbQuery = "tag:k-pop AND artist:$encodedName"
            val encodedQuery = URLEncoder.encode(mbQuery, "UTF-8")

            val url =
                "https://musicbrainz.org/ws/2/artist?query=$encodedQuery&fmt=json"

            val response = URL(url).readText()

            val root = JSONObject(response)
            val artistArray = root.optJSONArray("artists") ?: run {
                return@withContext ArtistResult.Success(emptyList())
            }
            ArtistResult.Success(emptyList())

            val list = mutableListOf<KpopArtist>()

            for (i in 0 until artistArray.length()) {
                val obj = artistArray.getJSONObject(i)
                val id = obj.optString("id", "")
                val name = obj.optString("name", "")
                val country = obj.optString("country", "")
                val disambiguation = obj.optString("disambiguation", "")
                val type = obj.optString("type", "")

                if (name.isNotBlank()) {
                    list.add(
                        KpopArtist(
                            id = id,
                            name = name,
                            country = country.ifBlank { null },
                            disambiguation = disambiguation.ifBlank { null },
                            type = type.ifBlank { null }
                        )
                    )
                }
            }

            ArtistResult.Success(list)
        } catch (e: Exception) {
            e.printStackTrace()
            ArtistResult.Error("Error while contacting MusicBrainz API.")
        }
    }
}