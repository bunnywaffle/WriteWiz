package com.example.ui.workshop

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.GeminiClient
import com.example.data.Chapter
import com.example.data.Section
import com.example.ui.components.MarkdownViewer
import com.example.ui.viewmodel.StoryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryWorkshopScreen(
    viewModel: StoryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val story by viewModel.currentStory.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val currentChapter by viewModel.currentChapter.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val currentSection by viewModel.currentSection.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Side sheets / Bottom sheets state for MOBILE
    var showOutlineSheet by remember { mutableStateOf(false) }
    var showAiSheet by remember { mutableStateOf(false) }

    // Dialog state for modifying chapters/sections
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var showAddSectionDialog by remember { mutableStateOf(false) }
    var showRenameChapterDialog by remember { mutableStateOf(currentChapter == null) }
    var showRenameSectionDialog by remember { mutableStateOf(currentSection == null) }

    var editingChapter by remember { mutableStateOf<Chapter?>(null) }
    var editingSection by remember { mutableStateOf<Section?>(null) }

    // Editor Text Field State in Workshop
    var editorValue by remember { mutableStateOf(TextFieldValue("")) }

    // Synchronize editor text value when active section changed in view model
    LaunchedEffect(currentSection) {
        currentSection?.let {
            if (editorValue.text != it.content) {
                editorValue = TextFieldValue(
                    text = it.content,
                    selection = TextRange(it.content.length)
                )
            }
        } ?: run {
            editorValue = TextFieldValue("")
        }
    }

    // Keep DB updated on edit value change
    fun updateSectionContentAndSave(newVal: TextFieldValue) {
        editorValue = newVal
        currentSection?.let {
            viewModel.updateSectionContent(newVal.text)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = story?.title ?: "Writing Workshop",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${story?.genre} • " + (currentChapter?.title ?: "No Chapter selected"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Library"
                        )
                    }
                },
                actions = {
                    // Manual Save State feedback indicator
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Saved successfully",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Autosaved",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = { showOutlineSheet = true }) {
                        Icon(imageVector = Icons.Filled.FormatListBulleted, contentDescription = "Manuscript Outline")
                    }

                    IconButton(onClick = { showAiSheet = true }) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "AI Assistant Hub",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Markdown helpful syntax tool row for typing support
            if (currentSection != null) {
                MarkdownHelperRow(
                    editorValue = editorValue,
                    onValueChange = { updateSectionContentAndSave(it) }
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            val isTablet = maxWidth > 840.dp

            Row(modifier = Modifier.fillMaxSize()) {
                // Expanded screen split-outline Left side sheet
                if (isTablet) {
                    Card(
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        OutlineNavigationPanel(
                            chapters = chapters,
                            currentChapter = currentChapter,
                            sections = sections,
                            currentSection = currentSection,
                            onSelectChapter = { viewModel.selectChapter(it) },
                            onSelectSection = { viewModel.selectSection(it) },
                            onCreateChapter = { showAddChapterDialog = true },
                            onCreateSection = { showAddSectionDialog = true },
                            onRenameChapter = {
                                editingChapter = it
                                showRenameChapterDialog = true
                            },
                            onRenameSection = {
                                editingSection = it
                                showRenameSectionDialog = true
                            },
                            onDeleteChapter = { viewModel.deleteChapter(it) },
                            onDeleteSection = { viewModel.deleteSection(it) }
                        )
                    }
                }

                // Center Writing workshop core area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (currentSection == null) {
                        WorkshopEmptyState(
                            hasChapters = chapters.isNotEmpty(),
                            onCreateChapter = { showAddChapterDialog = true },
                            onCreateSection = { showAddSectionDialog = true }
                        )
                    } else {
                        WorkshopEditorArea(
                            section = currentSection!!,
                            editorValue = editorValue,
                            onValueChange = { updateSectionContentAndSave(it) }
                        )
                    }
                }

                // Expanded screen split-assistant Right side sheet
                if (isTablet) {
                    Card(
                        modifier = Modifier
                            .width(340.dp)
                            .fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        AiAssistantPanel(
                            viewModel = viewModel,
                            editorValue = editorValue,
                            onInsertOutput = { text ->
                                val cursorPosition = editorValue.selection.max
                                val currentText = editorValue.text
                                val newText = if (cursorPosition in 0..currentText.length) {
                                    currentText.substring(0, cursorPosition) + "\n\n" + text + currentText.substring(cursorPosition)
                                } else {
                                    currentText + "\n\n" + text
                                }
                                updateSectionContentAndSave(
                                    editorValue.copy(
                                        text = newText,
                                        selection = TextRange(cursorPosition + text.length + 2)
                                    )
                                )
                            },
                            onReplaceOutput = { text ->
                                val start = editorValue.selection.min
                                val end = editorValue.selection.max
                                val currentText = editorValue.text
                                if (start != end && start >= 0 && end <= currentText.length) {
                                    val newText = currentText.substring(0, start) + text + currentText.substring(end)
                                    updateSectionContentAndSave(
                                        editorValue.copy(
                                            text = newText,
                                            selection = TextRange(start + text.length)
                                        )
                                    )
                                } else {
                                    // Fallback to append if nothing is highlighted
                                    val newText = currentText + "\n\n" + text
                                    updateSectionContentAndSave(
                                        editorValue.copy(
                                            text = newText,
                                            selection = TextRange(newText.length)
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // --- MOBILE Bottom Sheet Outline ---
            if (!isTablet && showOutlineSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showOutlineSheet = false },
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.8f)
                            .fillMaxWidth()
                    ) {
                        OutlineNavigationPanel(
                            chapters = chapters,
                            currentChapter = currentChapter,
                            sections = sections,
                            currentSection = currentSection,
                            onSelectChapter = {
                                viewModel.selectChapter(it)
                                showOutlineSheet = false
                            },
                            onSelectSection = {
                                viewModel.selectSection(it)
                                showOutlineSheet = false
                            },
                            onCreateChapter = { showAddChapterDialog = true },
                            onCreateSection = { showAddSectionDialog = true },
                            onRenameChapter = {
                                editingChapter = it
                                showRenameChapterDialog = true
                            },
                            onRenameSection = {
                                editingSection = it
                                showRenameSectionDialog = true
                            },
                            onDeleteChapter = { viewModel.deleteChapter(it) },
                            onDeleteSection = { viewModel.deleteSection(it) }
                        )
                    }
                }
            }

            // --- MOBILE Bottom Sheet AI Assistant ---
            if (!isTablet && showAiSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAiSheet = false },
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.8f)
                            .fillMaxWidth()
                    ) {
                        AiAssistantPanel(
                            viewModel = viewModel,
                            editorValue = editorValue,
                            onInsertOutput = { text ->
                                val cursorPosition = editorValue.selection.max
                                val currentText = editorValue.text
                                val newText = if (cursorPosition in 0..currentText.length) {
                                    currentText.substring(0, cursorPosition) + "\n\n" + text + currentText.substring(cursorPosition)
                                } else {
                                    currentText + "\n\n" + text
                                }
                                updateSectionContentAndSave(
                                    editorValue.copy(
                                        text = newText,
                                        selection = TextRange(cursorPosition + text.length + 2)
                                    )
                                )
                                showAiSheet = false
                            },
                            onReplaceOutput = { text ->
                                val start = editorValue.selection.min
                                val end = editorValue.selection.max
                                val currentText = editorValue.text
                                if (start != end && start >= 0 && end <= currentText.length) {
                                    val newText = currentText.substring(0, start) + text + currentText.substring(end)
                                    updateSectionContentAndSave(
                                        editorValue.copy(
                                            text = newText,
                                            selection = TextRange(start + text.length)
                                        )
                                    )
                                } else {
                                    val newText = currentText + "\n\n" + text
                                    updateSectionContentAndSave(
                                        editorValue.copy(
                                            text = newText,
                                            selection = TextRange(newText.length)
                                        )
                                    )
                                }
                                showAiSheet = false
                            }
                        )
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    if (showAddChapterDialog) {
        var chapTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddChapterDialog = false },
            title = { Text("New Chapter") },
            text = {
                OutlinedTextField(
                    value = chapTitle,
                    onValueChange = { chapTitle = it },
                    label = { Text("Chapter Title") },
                    placeholder = { Text("e.g. Chapter 2: Echoes of Danger") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (chapTitle.trim().isNotEmpty()) {
                            viewModel.createChapter(chapTitle.trim())
                            showAddChapterDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddChapterDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddSectionDialog) {
        var secTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSectionDialog = false },
            title = { Text("New Scene Section") },
            text = {
                OutlinedTextField(
                    value = secTitle,
                    onValueChange = { secTitle = it },
                    label = { Text("Section Title") },
                    placeholder = { Text("e.g. Nightfall Encounter") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (secTitle.trim().isNotEmpty()) {
                            viewModel.createSection(secTitle.trim())
                            showAddSectionDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSectionDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRenameChapterDialog && editingChapter != null) {
        var chapTitle by remember { mutableStateOf(editingChapter!!.title) }
        AlertDialog(
            onDismissRequest = { showRenameChapterDialog = false },
            title = { Text("Rename Chapter") },
            text = {
                OutlinedTextField(
                    value = chapTitle,
                    onValueChange = { chapTitle = it },
                    label = { Text("Chapter Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (chapTitle.trim().isNotEmpty()) {
                            viewModel.renameChapter(editingChapter!!, chapTitle.trim())
                            showRenameChapterDialog = false
                            editingChapter = null
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRenameChapterDialog = false
                    editingChapter = null
                }) { Text("Cancel") }
            }
        )
    }

    if (showRenameSectionDialog && editingSection != null) {
        var secTitle by remember { mutableStateOf(editingSection!!.title) }
        AlertDialog(
            onDismissRequest = { showRenameSectionDialog = false },
            title = { Text("Rename Section") },
            text = {
                OutlinedTextField(
                    value = secTitle,
                    onValueChange = { secTitle = it },
                    label = { Text("Section Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (secTitle.trim().isNotEmpty()) {
                            viewModel.renameSection(editingSection!!, secTitle.trim())
                            showRenameSectionDialog = false
                            editingSection = null
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRenameSectionDialog = false
                    editingSection = null
                }) { Text("Cancel") }
            }
        )
    }
}

// --- Manuscript Outline Navigation Panel ---

@Composable
fun OutlineNavigationPanel(
    chapters: List<Chapter>,
    currentChapter: Chapter?,
    sections: List<Section>,
    currentSection: Section?,
    onSelectChapter: (Chapter) -> Unit,
    onSelectSection: (Section) -> Unit,
    onCreateChapter: () -> Unit,
    onCreateSection: () -> Unit,
    onRenameChapter: (Chapter) -> Unit,
    onRenameSection: (Section) -> Unit,
    onDeleteChapter: (Chapter) -> Unit,
    onDeleteSection: (Section) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MANUSCRIPT",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            TextButton(
                onClick = onCreateChapter,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Chapter", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Chapter", style = MaterialTheme.typography.labelSmall)
            }
        }

        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (chapters.isEmpty()) {
                item {
                    Text(
                        text = "No chapters created yet. Tap '+ Chapter' to create one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            chapters.forEach { chap ->
                val isSelectedChap = currentChapter?.id == chap.id

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectChapter(chap) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelectedChap) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            } else {
                                Color.Transparent
                            }
                        ),
                        border = if (isSelectedChap) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        } else null
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isSelectedChap) Icons.Filled.FolderOpen else Icons.Filled.Folder,
                                        contentDescription = "Chapter folder",
                                        tint = if (isSelectedChap) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = chap.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelectedChap) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelectedChap) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = { onRenameChapter(chap) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "Rename", modifier = Modifier.size(14.dp))
                                    }
                                    IconButton(
                                        onClick = { onDeleteChapter(chap) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            // Render Sections directly under the active/expanded Chapter
                            if (isSelectedChap) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Scenes",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                    TextButton(
                                        onClick = onCreateSection,
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "Add Scene", modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Add Scene", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                                    }
                                }

                                if (sections.isEmpty()) {
                                    Text(
                                        text = "No scenes. Add one to start writing.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontStyle = FontStyle.Italic,
                                        modifier = Modifier.padding(start = 24.dp, top = 4.dp, bottom = 4.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    sections.forEach { sec ->
                                        val isSelectedSec = currentSection?.id == sec.id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 12.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (isSelectedSec) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                    else Color.Transparent
                                                )
                                                .clickable { onSelectSection(sec) }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Notes,
                                                    contentDescription = "Scene notes icon",
                                                    tint = if (isSelectedSec) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = sec.title,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (isSelectedSec) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelectedSec) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                IconButton(
                                                    onClick = { onRenameSection(sec) },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Filled.Edit, contentDescription = "Rename Scene", modifier = Modifier.size(12.dp))
                                                }
                                                IconButton(
                                                    onClick = { onDeleteSection(sec) },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete Scene", modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Empty Workshop Panel state ---

@Composable
fun WorkshopEmptyState(
    hasChapters: Boolean,
    onCreateChapter: () -> Unit,
    onCreateSection: () -> Unit,
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
            modifier = Modifier.widthIn(max = 380.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AutoStories,
                contentDescription = "No scene active",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(72.dp)
            )

            Text(
                text = "Open Your Outline",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (!hasChapters) {
                    "Your manuscript is currently blank. Let's design the story skeleton by creating your first chapter!"
                } else {
                    "Select an existing Scene Section in the Outline menu, or create a brand new scene to launch the Markdown Editor."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            if (!hasChapters) {
                Button(
                    onClick = onCreateChapter,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Chapter")
                }
            } else {
                Button(
                    onClick = onCreateSection,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Scene Section")
                }
            }
        }
    }
}

// --- Dynamic Core Writing Area ---

@Composable
fun WorkshopEditorArea(
    section: Section,
    editorValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0 -> Editor (Write), 1 -> Dynamic Preview

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Tab Headers
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.EditNote, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Draft (Write)")
                    }
                }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Book, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Previsualize (Read)")
                    }
                }
            )
        }

        // Selected Content Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (activeTab == 0) {
                // WRITE EDITOR (Custom basic Rich text field with beautiful layout)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    BasicTextField(
                        value = editorValue,
                        onValueChange = { onValueChange(it) },
                        modifier = Modifier
                            .fillMaxSize(),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 28.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (editorValue.text.isEmpty()) {
                                Text(
                                    text = "Start typing Chapter beats, using pure Markdown (e.g. # Heading, **bold**, *italic*)...",
                                    style = TextStyle(
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        lineHeight = 28.sp
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            } else {
                // PARSED BEAUTIFUL PREVIEW CONTENT
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            // Luxurious warm parchment tone or soft card back
                            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                        )
                        .verticalScroll(rememberScrollState())
                ) {
                    MarkdownViewer(
                        markdown = editorValue.text,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// --- Keyboard Helper Formatting Toolbar ---

@Composable
fun MarkdownHelperRow(
    editorValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    fun insertFormat(prefix: String, suffix: String = "") {
        val selection = editorValue.selection
        val text = editorValue.text
        val selectedText = text.substring(selection.min, selection.max)
        val newText = text.substring(0, selection.min) + prefix + selectedText + suffix + text.substring(selection.max)
        val newSelection = if (selection.collapsed) {
            TextRange(selection.min + prefix.length)
        } else {
            TextRange(selection.min, selection.min + prefix.length + selectedText.length + suffix.length)
        }
        onValueChange(editorValue.copy(text = newText, selection = newSelection))
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { insertFormat("# ", "") }) {
                Text("H1", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { insertFormat("## ", "") }) {
                Text("H2", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { insertFormat("**", "**") }) {
                Icon(imageVector = Icons.Filled.FormatBold, contentDescription = "Bold", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { insertFormat("*", "*") }) {
                Icon(imageVector = Icons.Filled.FormatItalic, contentDescription = "Italic", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { insertFormat("> ", "") }) {
                Icon(imageVector = Icons.Filled.FormatQuote, contentDescription = "Quote", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { insertFormat("- ", "") }) {
                Icon(imageVector = Icons.Filled.FormatListBulleted, contentDescription = "List", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = {
                onValueChange(
                    editorValue.copy(
                        text = editorValue.text + "\n",
                        selection = TextRange(editorValue.text.length + 1)
                    )
                )
            }) {
                Icon(imageVector = Icons.Filled.KeyboardReturn, contentDescription = "New Line", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// --- AI Assistant Toolbox Side Sheet ---

@Composable
fun AiAssistantPanel(
    viewModel: StoryViewModel,
    editorValue: TextFieldValue,
    onInsertOutput: (String) -> Unit,
    onReplaceOutput: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) } // 0 -> Actions, 1 -> Last Result

    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiOutput by viewModel.aiOutput.collectAsState()
    val aiError by viewModel.aiError.collectAsState()

    var customRefineInput by remember { mutableStateOf("") }
    var selectedTone by remember { mutableStateOf("Victorian Novelist") }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Observe AI activity, send user automatically to the "Result" view tab when generation completes successfully!
    LaunchedEffect(aiOutput) {
        if (aiOutput != null) {
            selectedTab = 1
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = "AI MUSE WORKBENCH",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Spells", modifier = Modifier.padding(vertical = 10.dp), style = MaterialTheme.typography.labelMedium)
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Parchment Output", style = MaterialTheme.typography.labelMedium)
                    if (aiOutput != null) {
                        Box(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            // SPELLS ACTIONS (AI Features)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Spell 1: Continue story based on selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.Autorenew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Continuous Ink Flow", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Gemini will absorb the story's context, outline, and preceding prose, generating the next continuous sensory paragraphs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val textContext = if (editorValue.text.isNotEmpty()) {
                                    // Use cursor position context or whole text
                                    val cursor = editorValue.selection.max
                                    if (cursor > 200) editorValue.text.substring(cursor - 200, cursor)
                                    else editorValue.text.substring(0, cursor)
                                } else ""
                                viewModel.aiContinueWriting(textContext)
                            },
                            enabled = !isAiLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Flow Next Paragraph")
                        }
                    }
                }

                // Spell 2: Refine / Polish drafts
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Refine Selected Draft", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Highlight a section in your editor and choose an instruction (or type standard notes below) to refine.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val selectedText = if (editorValue.selection.min != editorValue.selection.max) {
                            editorValue.text.substring(editorValue.selection.min, editorValue.selection.max)
                        } else ""

                        OutlinedTextField(
                            value = customRefineInput,
                            onValueChange = { customRefineInput = it },
                            placeholder = { Text("e.g. Add subtle foreshadowing and details") },
                            label = { Text("Custom Refinement Instruction") },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = {
                                    val promptStr = customRefineInput.ifEmpty { "Enhance pacing and sensory grammar" }
                                    val targetText = selectedText.ifEmpty { editorValue.text }
                                    viewModel.aiRefineContent(targetText, promptStr)
                                },
                                enabled = !isAiLoading,
                                modifier = Modifier.weight(1f)
                              ) {
                                Text("Refine", fontSize = 12.sp)
                            }
                            
                            Button(
                                onClick = {
                                    viewModel.aiGetFeedback(selectedText.ifEmpty { editorValue.text })
                                },
                                enabled = !isAiLoading,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Critique Scene", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Spell 3: Style Transmuter (Tone Changer)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.Style, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Prose Tone Transmuter", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Reauthor the drafted paragraphs into specific historical or theatrical writing styles.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val tones = listOf("Dark Gothic Storyteller", "Noir Detective Novelist", "Minimalist (Hemingway)", "Whimsical Fairy Tale", "Epic Fantasy Bard")
                        var expandedToneDropdown by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandedToneDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(selectedTone)
                                Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = expandedToneDropdown,
                                onDismissRequest = { expandedToneDropdown = false }
                            ) {
                                tones.forEach { tone ->
                                    DropdownMenuItem(
                                        text = { Text(tone) },
                                        onClick = {
                                            selectedTone = tone
                                            expandedToneDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val selectedText = if (editorValue.selection.min != editorValue.selection.max) {
                                    editorValue.text.substring(editorValue.selection.min, editorValue.selection.max)
                                } else ""
                                viewModel.aiRewriteWithTone(selectedText.ifEmpty { editorValue.text }, selectedTone)
                            },
                            enabled = !isAiLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Apply Transmutation")
                        }
                    }
                }
            }
        } else {
            // OUTPUT PARCHMENT PANEL
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isAiLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Gemini is composing your draft...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (aiError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                            Text(
                                text = aiError!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (aiOutput != null) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Scrolling result viewer
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f))
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            MarkdownViewer(markdown = aiOutput!!)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action rows to insert
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onInsertOutput(aiOutput!!) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Filled.Input, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Insert", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { onReplaceOutput(aiOutput!!) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Filled.FindReplace, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Replace Highlight", fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.alpha(0.6f)
                        ) {
                            Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            Text(
                                text = "Select a writing spell and request magic from your muse assistant.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }
    }
}
