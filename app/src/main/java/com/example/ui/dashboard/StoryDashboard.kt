package com.example.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Story

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryDashboardScreen(
    stories: List<Story>,
    onSelectStory: (Story) -> Unit,
    onCreateStory: (String, String, String) -> Unit,
    onDeleteStory: (Story) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoStories,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "MuseWriter",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Serif
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "New Novel Story")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                text = { Text("New Manuscript") },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (stories.isEmpty()) {
                DashboardEmptyState(onCreateRequest = { showCreateDialog = true })
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Header Card with beautiful Gradient
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                )
                                .padding(24.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Text(
                                        text = "CREATIVE WORKSHOP ACTIVE",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                    )
                                }
                                Text(
                                    text = "Welcome back, Author",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "Manage book outlines, section story drafts, and use Gemini to flow continuations or edit pacing on your manuscripts.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = "MANUSCRIPTS IN PROGRESS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Books Grid list
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(stories) { story ->
                            StoryCard(
                                story = story,
                                onClick = { onSelectStory(story) },
                                onDelete = { onDeleteStory(story) }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Create Novel Manuscript Dialog ---
    if (showCreateDialog) {
        var title by remember { mutableStateOf("") }
        var genre by remember { mutableStateOf("Fantasy") }
        var synopsis by remember { mutableStateOf("") }

        val genres = listOf("Fantasy", "Sci-Fi", "Mystery", "Thriller", "Romance", "Historical Fiction", "Drama", "Self-Help")

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    text = "Craft New Novel Manuscript",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Book Title") },
                        placeholder = { Text("e.g. Chronicles of the Emerald Peak") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column {
                        Text(
                            text = "Genre Topic badge:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            genres.forEach { g ->
                                FilterChip(
                                    selected = genre == g,
                                    onClick = { genre = g },
                                    label = { Text(g) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = synopsis,
                        onValueChange = { synopsis = it },
                        label = { Text("Book Synopsis / Overview") },
                        placeholder = { Text("Outline the plot setting, main characters, and conflict context for the AI assistant...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.trim().isNotEmpty() && synopsis.trim().isNotEmpty()) {
                            onCreateStory(title.trim(), genre, synopsis.trim())
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Begin Writing")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// --- Dashboard Empty State ---

@Composable
fun DashboardEmptyState(
    onCreateRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.widthIn(max = 360.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.LibraryBooks,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                modifier = Modifier.size(80.dp)
            )

            Text(
                text = "Empty Bookshelves",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif
            )

            Text(
                text = "Begin your next literary masterpiece! Create a story manuscript, set your genre prompts, and unlock chapter outlines and paragraph editors.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Button(
                onClick = onCreateRequest,
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Write Your Story")
            }
        }
    }
}

// --- Story card item in dashboard grid listing ---

@Composable
fun StoryCard(
    story: Story,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = story.genre,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.offset(x = 8.dp, y = (-8).dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete Draft Book",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = story.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = story.synopsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Open Manuscript",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Filled.AutoStories,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
