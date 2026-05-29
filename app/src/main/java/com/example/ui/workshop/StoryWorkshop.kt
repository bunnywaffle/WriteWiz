package com.example.ui.workshop

import android.os.Environment
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.ui.theme.ThemeSelectorDialog
import com.example.ui.viewmodel.StoryViewModel
import com.example.utils.ExportUtils
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

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
    val context = LocalContext.current

    // Dialog state
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var showAddSectionDialog by remember { mutableStateOf(false) }
    var showRenameChapterDialog by remember { mutableStateOf(false) }
    var showRenameSectionDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    var editingChapter by remember { mutableStateOf<Chapter?>(null) }
    var editingSection by remember { mutableStateOf<Section?>(null) }

    // Text Reader System (TTS) native android integrations
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsListening by remember { mutableStateOf(false) }
    var pitchTts by remember { mutableFloatStateOf(1.0f) }
    var speedTts by remember { mutableFloatStateOf(1.0f) }

    DisposableEffect(context) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { safeTts ->
                    safeTts.language = Locale.US
                    safeTts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            (context as? android.app.Activity)?.runOnUiThread {
                                isTtsListening = true
                            }
                        }
                        override fun onDone(utteranceId: String?) {
                            (context as? android.app.Activity)?.runOnUiThread {
                                isTtsListening = false
                            }
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            (context as? android.app.Activity)?.runOnUiThread {
                                isTtsListening = false
                            }
                        }
                    })
                }
            }
        }
        ttsInstance = tts
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // Toggle Play/Stop Narrator
    fun toggleTextReader(speechText: String) {
        val speech = ttsInstance
        if (speech == null) {
            Toast.makeText(context, "Narrator voice initializing, please try again", Toast.LENGTH_SHORT).show()
            return
        }

        if (isTtsListening) {
            speech.stop()
            isTtsListening = false
        } else {
            if (speechText.trim().isEmpty()) {
                Toast.makeText(context, "No text draft available to read", Toast.LENGTH_SHORT).show()
                return
            }
            speech.setPitch(pitchTts)
            speech.setSpeechRate(speedTts)
            speech.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "ManuscriptSpeak")
            isTtsListening = true
        }
    }

    // Mobile side panels toggles
    var showOutlineSheet by remember { mutableStateOf(false) }
    var showAiSheet by remember { mutableStateOf(false) }

    // Active Editor Text Fields State
    var editorValue by remember { mutableStateOf(TextFieldValue("")) }

    // Sync state layout changes
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
        if (isTtsListening) {
            ttsInstance?.stop()
            isTtsListening = false
        }
    }

    // Live word statistic counts of active scene segment block
    val rawText = editorValue.text
    val activeWordCount = remember(rawText) {
        rawText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }
    val activeCharCount = remember(rawText) {
        rawText.length
    }
    val estimatedReadTime = remember(activeWordCount) {
        (activeWordCount / 200).coerceAtLeast(1)
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
                            fontFamily = FontFamily.Serif,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val genreSubtitle = if (!story?.genre.isNullOrBlank()) "${story?.genre} • " else ""
                        Text(
                            text = genreSubtitle + (currentChapter?.title ?: "Select Chapter"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariantByAlpha(0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Return to Dashboard")
                    }
                },
                actions = {
                    // Theme color selector
                    IconButton(onClick = { showThemeDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.ColorLens,
                            contentDescription = "Customize Theme Color",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Export manuscript button
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.IosShare,
                            contentDescription = "Export Manuscript Package",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { showOutlineSheet = true }) {
                        Icon(imageVector = Icons.Filled.FormatListBulleted, contentDescription = "Sitemap Chapters")
                    }

                    IconButton(onClick = { showAiSheet = true }) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "AI Editor spell bench",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            if (currentSection != null) {
                Column {
                    // Word numbers & metrics row bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 1.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.Numbers, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = "$activeWordCount words",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.TextFields, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "$activeCharCount chars",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "~$estimatedReadTime min read",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            // Visual Save Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                                Text(
                                    text = "Autosaved",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Native Markdown helpers toolbar layout formatting help
                    MarkdownHelperRow(
                        editorValue = editorValue,
                        onValueChange = { editorValue = it; currentSection?.let { sec -> viewModel.updateSectionContent(it.text) } }
                    )
                }
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
                // Tablet navigation outline split layout left pane
                if (isTablet) {
                    Card(
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                        shape = RoundedCornerShape(0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
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
                            onRenameChapter = { editingChapter = it; showRenameChapterDialog = true },
                            onRenameSection = { editingSection = it; showRenameSectionDialog = true },
                            onDeleteChapter = { viewModel.deleteChapter(it) },
                            onDeleteSection = { viewModel.deleteSection(it) },
                            onMoveChapterUp = { viewModel.moveChapterUp(it) },
                            onMoveChapterDown = { viewModel.moveChapterDown(it) },
                            onMoveSectionUp = { viewModel.moveSectionUp(it) },
                            onMoveSectionDown = { viewModel.moveSectionDown(it) }
                        )
                    }
                }

                // Core draft text workshop pane elements
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    if (currentSection == null) {
                        WorkshopEmptyState(
                            hasChapters = chapters.isNotEmpty(),
                            onCreateChapter = { showAddChapterDialog = true },
                            onCreateSection = { showAddSectionDialog = true }
                        )
                    } else {
                        // Text player controls / TTS Narrator panel box
                        TextReaderControls(
                            isPlaying = isTtsListening,
                            pitch = pitchTts,
                            speed = speedTts,
                            onPitchChange = { pitchTts = it },
                            onSpeedChange = { speedTts = it },
                            onTogglePlay = { toggleTextReader(editorValue.text) }
                        )

                        WorkshopEditorArea(
                            section = currentSection!!,
                            editorValue = editorValue,
                            onValueChange = { 
                                editorValue = it
                                viewModel.updateSectionContent(it.text)
                            }
                        )
                    }
                }

                // Tablet edit and helper panel right pane
                if (isTablet) {
                    Card(
                        modifier = Modifier
                            .width(360.dp)
                            .fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                        shape = RoundedCornerShape(0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    ) {
                        AiAssistantPanel(
                            viewModel = viewModel,
                            editorValue = editorValue,
                            onInsertOutput = { text ->
                                val cursor = editorValue.selection.max
                                val original = editorValue.text
                                val updated = if (cursor in 0..original.length) {
                                    original.substring(0, cursor) + "\n" + text + original.substring(cursor)
                                } else {
                                    original + "\n" + text
                                }
                                editorValue = editorValue.copy(text = updated, selection = TextRange(cursor + text.length + 1))
                                viewModel.updateSectionContent(updated)
                            },
                            onReplaceOutput = { text ->
                                val start = editorValue.selection.min
                                val end = editorValue.selection.max
                                val original = editorValue.text
                                if (start != end && start >= 0 && end <= original.length) {
                                    val updated = original.substring(0, start) + text + original.substring(end)
                                    editorValue = editorValue.copy(text = updated, selection = TextRange(start + text.length))
                                    viewModel.updateSectionContent(updated)
                                } else {
                                    val updated = original + "\n" + text
                                    editorValue = editorValue.copy(text = updated, selection = TextRange(updated.length))
                                    viewModel.updateSectionContent(updated)
                                }
                            }
                        )
                    }
                }
            }

            // --- Mobile Sheets fallback draw ---
            if (!isTablet && showOutlineSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showOutlineSheet = false },
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Box(modifier = Modifier.fillMaxHeight(0.85f).fillMaxWidth()) {
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
                            onRenameChapter = { editingChapter = it; showRenameChapterDialog = true },
                            onRenameSection = { editingSection = it; showRenameSectionDialog = true },
                            onDeleteChapter = { viewModel.deleteChapter(it) },
                            onDeleteSection = { viewModel.deleteSection(it) },
                            onMoveChapterUp = { viewModel.moveChapterUp(it) },
                            onMoveChapterDown = { viewModel.moveChapterDown(it) },
                            onMoveSectionUp = { viewModel.moveSectionUp(it) },
                            onMoveSectionDown = { viewModel.moveSectionDown(it) }
                        )
                    }
                }
            }

            if (!isTablet && showAiSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAiSheet = false },
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Box(modifier = Modifier.fillMaxHeight(0.85f).fillMaxWidth()) {
                        AiAssistantPanel(
                            viewModel = viewModel,
                            editorValue = editorValue,
                            onInsertOutput = { text ->
                                val cursor = editorValue.selection.max
                                val original = editorValue.text
                                val updated = if (cursor in 0..original.length) {
                                    original.substring(0, cursor) + "\n" + text + original.substring(cursor)
                                } else {
                                    original + "\n" + text
                                }
                                editorValue = editorValue.copy(text = updated, selection = TextRange(cursor + text.length + 1))
                                viewModel.updateSectionContent(updated)
                                showAiSheet = false
                            },
                            onReplaceOutput = { text ->
                                val start = editorValue.selection.min
                                val end = editorValue.selection.max
                                val original = editorValue.text
                                if (start != end && start >= 0 && end <= original.length) {
                                    val updated = original.substring(0, start) + text + original.substring(end)
                                    editorValue = editorValue.copy(text = updated, selection = TextRange(start + text.length))
                                    viewModel.updateSectionContent(updated)
                                } else {
                                    val updated = original + "\n" + text
                                    editorValue = editorValue.copy(text = updated, selection = TextRange(updated.length))
                                    viewModel.updateSectionContent(updated)
                                }
                                showAiSheet = false
                            }
                        )
                    }
                }
            }
        }
    }

    if (showThemeDialog) {
        ThemeSelectorDialog(onDismiss = { showThemeDialog = false })
    }

    // --- Story/Manuscript HTML and PDF exports dialog box ---
    if (showExportDialog) {
        var isExportingAll by remember { mutableStateOf(true) } // true -> Full book, false -> Current scene section only

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Filled.HistoryEdu, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Export Manuscript", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Typeset your story into highly formatted publishing layouts, exporting natively directly to your Downloads folder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Export Scope Switches
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Export scope:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { isExportingAll = true },
                                modifier = Modifier.weight(1f),
                                border = if (isExportingAll) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isExportingAll) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent
                                )
                            ) {
                                Text("Full Manuscript", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { isExportingAll = false },
                                modifier = Modifier.weight(1f),
                                enabled = currentSection != null,
                                border = if (!isExportingAll) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (!isExportingAll) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent
                                )
                            ) {
                                Text("Current Scene", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    // Action formats select
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Select export action format below:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                        // 1. PDF export button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val docTitle = if (isExportingAll) story?.title ?: "Full Book" else currentSection?.title ?: "Scene Section"
                                    val compiledText = if (isExportingAll) viewModel.compileFullBookText() else currentSection?.content ?: ""

                                    val pdfFile = ExportUtils.generatePdf(context, docTitle, compiledText)
                                    val saved = saveFileToPublicDownloads(context, pdfFile, "${docTitle.replace("\\s+".toRegex(), "_")}.pdf")
                                    if (saved) showExportDialog = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Professional PDF (A4)")
                        }

                        // 2. DOCX Word file export button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val docTitle = if (isExportingAll) story?.title ?: "Full Book" else currentSection?.title ?: "Scene Section"
                                    val compiledText = if (isExportingAll) viewModel.compileFullBookText() else currentSection?.content ?: ""

                                    val docxFile = ExportUtils.generateDocx(context, docTitle, compiledText)
                                    val saved = saveFileToPublicDownloads(context, docxFile, "${docTitle.replace("\\s+".toRegex(), "_")}.docx")
                                    if (saved) showExportDialog = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(imageVector = Icons.Filled.Article, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Microsoft Word (.docx)")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Close") }
            }
        )
    }

    // Standard Chapter renaming / Section dialogues
    if (showAddChapterDialog) {
        var textValue by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddChapterDialog = false },
            title = { Text("Add New Chapter") },
            text = {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("Chapter Title") },
                    placeholder = { Text("e.g. Chapter 2: The Return") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textValue.trim().isNotEmpty()) {
                            viewModel.createChapter(textValue.trim())
                            showAddChapterDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAddChapterDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddSectionDialog) {
        var textValue by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSectionDialog = false },
            title = { Text("New Scene Section") },
            text = {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("Scene Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textValue.trim().isNotEmpty()) {
                            viewModel.createSection(textValue.trim())
                            showAddSectionDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSectionDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRenameChapterDialog && editingChapter != null) {
        var textValue by remember { mutableStateOf(editingChapter!!.title) }
        AlertDialog(
            onDismissRequest = { showRenameChapterDialog = false; editingChapter = null },
            title = { Text("Rename Chapter") },
            text = {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("Chapter Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textValue.trim().isNotEmpty()) {
                            viewModel.renameChapter(editingChapter!!, textValue.trim())
                            showRenameChapterDialog = false
                            editingChapter = null
                        }
                    }
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameChapterDialog = false; editingChapter = null }) { Text("Cancel") }
            }
        )
    }

    if (showRenameSectionDialog && editingSection != null) {
        var textValue by remember { mutableStateOf(editingSection!!.title) }
        AlertDialog(
            onDismissRequest = { showRenameSectionDialog = false; editingSection = null },
            title = { Text("Rename Scene") },
            text = {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("Scene Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textValue.trim().isNotEmpty()) {
                            viewModel.renameSection(editingSection!!, textValue.trim())
                            showRenameSectionDialog = false
                            editingSection = null
                        }
                    }
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameSectionDialog = false; editingSection = null }) { Text("Cancel") }
            }
        )
    }
}

// Helper methods to copy cache exports safely to the public Download directory of the platform Android
fun saveFileToPublicDownloads(context: android.content.Context, source: File, labelName: String): Boolean {
    return try {
        val destDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!destDir.exists()) destDir.mkdirs()
        val destFile = File(destDir, labelName)
        source.copyTo(destFile, overwrite = true)
        Toast.makeText(context, "Manuscript Export Finished! Files saved into Downloads folder.", Toast.LENGTH_LONG).show()
        true
    } catch (e: Exception) {
        Toast.makeText(context, "Saved internally: ${source.name}. Locate in app resources.", Toast.LENGTH_LONG).show()
        false
    }
}

// Alpha theme modifier resolver
@Composable
fun ColorScheme.onSurfaceVariantByAlpha(alpha: Float): Color {
    return this.onSurfaceVariant.copy(alpha = alpha)
}

// Modifier utility box scope
private fun Modifier.size(dp: Int): Modifier = this.size(dp.dp)

// --- Native Audio / Text Reader Settings Panel Controls ---
@Composable
fun TextReaderControls(
    isPlaying: Boolean,
    pitch: Float,
    speed: Float,
    onPitchChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onTogglePlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(1.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Square else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Read Scene Aloud",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isPlaying) "NARRATOR SPEAKING ALOUD" else "NATIVE MANUSCRIPT READER",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pitch slider Customise voice speed
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Voice Tone", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = pitch,
                            onValueChange = onPitchChange,
                            valueRange = 0.5f..1.5f,
                            modifier = Modifier.height(20.dp)
                        )
                    }

                    // Speed tempo slider
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Scribe Pace", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = speed,
                            onValueChange = onSpeedChange,
                            valueRange = 0.5f..1.5f,
                            modifier = Modifier.height(20.dp)
                        )
                    }
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
    var activeTab by remember { mutableStateOf(0) } // 0 -> Editor, 1 -> Markdown beautiful Previsualization

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Filled.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Draft Scene (Write)")
                    }
                }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Filled.ChromeReaderMode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Previsualizer (Read)")
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (activeTab == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    BasicTextField(
                        value = editorValue,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxSize(),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 28.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (editorValue.text.isEmpty()) {
                                Text(
                                    text = "Start writing chapter beats here using standard Markdown markup...\n\nUse headers (# Section), **Bold words**, or *Italics* dynamically.",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                        lineHeight = 28.sp
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
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
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { insertFormat("# ", "") }) {
                Text("H1", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { insertFormat("## ", "") }) {
                Text("H2", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { insertFormat("**", "**") }) {
                Icon(imageVector = Icons.Filled.FormatBold, contentDescription = "Bold", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { insertFormat("*", "*") }) {
                Icon(imageVector = Icons.Filled.FormatItalic, contentDescription = "Italic", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { insertFormat("`", "`") }) {
                Icon(imageVector = Icons.Filled.Code, contentDescription = "Code Block", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { insertFormat("- ", "") }) {
                Icon(imageVector = Icons.Filled.FormatListBulleted, contentDescription = "Unordered List", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { insertFormat("\n---\n", "") }) {
                Icon(imageVector = Icons.Filled.HorizontalRule, contentDescription = "Horizontal Line", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// --- Manuscript Outline Navigation Panel (Hierarchical structural chapters and sections) ---
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
    onDeleteSection: (Section) -> Unit,
    onMoveChapterUp: (Chapter) -> Unit,
    onMoveChapterDown: (Chapter) -> Unit,
    onMoveSectionUp: (Section) -> Unit,
    onMoveSectionDown: (Section) -> Unit
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
                text = "BOOK MAP LAYOUT",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            TextButton(
                onClick = onCreateChapter,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Chapter", style = MaterialTheme.typography.labelSmall)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (chapters.isEmpty()) {
                item {
                    Text(
                        text = "No book chapters created yet. Tap '+ Chapter' to seed the manuscript map.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            itemsIndexed(chapters) { index, chap ->
                val isSelectedChap = currentChapter?.id == chap.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectChapter(chap) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelectedChap) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        } else {
                            Color.Transparent
                        }
                    ),
                    border = if (isSelectedChap) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
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
                                    contentDescription = null,
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

                            // Dynamic chapters move reorder control sectioning row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (index > 0) {
                                    IconButton(onClick = { onMoveChapterUp(chap) }, modifier = Modifier.size(22.dp)) {
                                        Icon(imageVector = Icons.Filled.ArrowUpward, contentDescription = "Move Chapter Up", modifier = Modifier.size(12.dp))
                                    }
                                }
                                if (index < chapters.size - 1) {
                                    IconButton(onClick = { onMoveChapterDown(chap) }, modifier = Modifier.size(22.dp)) {
                                        Icon(imageVector = Icons.Filled.ArrowDownward, contentDescription = "Move Chapter Down", modifier = Modifier.size(12.dp))
                                    }
                                }
                                IconButton(onClick = { onRenameChapter(chap) }, modifier = Modifier.size(22.dp)) {
                                    Icon(imageVector = Icons.Filled.Edit, contentDescription = "Rename", modifier = Modifier.size(12.dp))
                                }
                                IconButton(onClick = { onDeleteChapter(chap) }, modifier = Modifier.size(22.dp)) {
                                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        // Sections Render list beneath selected parent expansion chapter index
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
                                    text = "Scenes Subsections",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                                TextButton(
                                    onClick = onCreateSection,
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Add Scene", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                                }
                            }

                            if (sections.isEmpty()) {
                                Text(
                                    text = "Blank chapter. Seed some scenes to draft.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier.padding(start = 24.dp, top = 6.dp, bottom = 6.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                sections.forEachIndexed { sIndex, sec ->
                                    val isSelectedSec = currentSection?.id == sec.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSelectedSec) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
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
                                                contentDescription = null,
                                                tint = if (isSelectedSec) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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

                                        // Scenes Up/down arrow order shifting selectors inside Book map
                                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                            if (sIndex > 0) {
                                                IconButton(onClick = { onMoveSectionUp(sec) }, modifier = Modifier.size(18.dp)) {
                                                    Icon(imageVector = Icons.Filled.ArrowUpward, contentDescription = "Move Scene Up", modifier = Modifier.size(10.dp))
                                                }
                                            }
                                            if (sIndex < sections.size - 1) {
                                                IconButton(onClick = { onMoveSectionDown(sec) }, modifier = Modifier.size(18.dp)) {
                                                    Icon(imageVector = Icons.Filled.ArrowDownward, contentDescription = "Move Scene Down", modifier = Modifier.size(10.dp))
                                                }
                                            }
                                            IconButton(onClick = { onRenameSection(sec) }, modifier = Modifier.size(18.dp)) {
                                                Icon(imageVector = Icons.Filled.Edit, contentDescription = "Rename", modifier = Modifier.size(10.dp))
                                            }
                                            IconButton(onClick = { onDeleteSection(sec) }, modifier = Modifier.size(18.dp)) {
                                                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.error)
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
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                modifier = Modifier.size(72.dp)
            )

            Text(
                text = "Manuscript Empty State",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (!hasChapters) {
                    "Your draft manuscript map has no chapters yet. Let's start typing the structure of the story by creating your first chapter!"
                } else {
                    "Excellent chapter folder is set up! Select a Scene Subsection in your Book Map navigation panel, or add a fresh scene to enable the Markdown draft editor."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            if (!hasChapters) {
                Button(
                    onClick = onCreateChapter,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Chapter Block")
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

// --- AI Assistant Toolbox Side Sheet & Grammar Spellchecker ---
@Composable
fun AiAssistantPanel(
    viewModel: StoryViewModel,
    editorValue: TextFieldValue,
    onInsertOutput: (String) -> Unit,
    onReplaceOutput: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 -> Spells, 1 -> Last Parchment output result

    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiOutput by viewModel.aiOutput.collectAsState()
    val aiError by viewModel.aiError.collectAsState()

    var customRefineInput by remember { mutableStateOf("") }
    var selectedTone by remember { mutableStateOf("Victorian Novelist") }

    // Toggle scroll result viewer tab on successful response
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
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Spells Toolbox", modifier = Modifier.padding(vertical = 10.dp), style = MaterialTheme.typography.labelMedium)
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Parchment Output", style = MaterialTheme.typography.labelMedium)
                    if (aiOutput != null) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Spell 0: AI Typo & Spell Grammar Proofer
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Filled.Spellcheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("AI Proofreader & Spellchecker", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Analyze active draft for hidden orthography and tense issues, returning a styled comparison report and polished text.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { viewModel.aiSpellCheck(editorValue.text) },
                            enabled = !isAiLoading && editorValue.text.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Spell Check Manuscript")
                        }
                    }
                }

                // Spell 1: Continue story flow
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Filled.Autorenew, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text("Continuous Ink Flow", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Gemini will absorb the preceding prose to continue writing the scenes draft seamlessly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                val cursor = editorValue.selection.max
                                val text = editorValue.text
                                val contextText = if (text.isNotEmpty() && cursor > 0) {
                                    if (cursor > 300) text.substring(cursor - 300, cursor)
                                    else text.substring(0, cursor)
                                } else text
                                viewModel.aiContinueWriting(contextText)
                            },
                            enabled = !isAiLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Flow Next Paragraph")
                        }
                    }
                }

                // Spell 2: Refining selected chunks
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text("Rewrite & Critique segment", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Highlight draft pieces inside your editor to polish pacing or generate visual structural feedback.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val highlighted = if (editorValue.selection.min != editorValue.selection.max) {
                            editorValue.text.substring(editorValue.selection.min, editorValue.selection.max)
                        } else ""

                        OutlinedTextField(
                            value = customRefineInput,
                            onValueChange = { customRefineInput = it },
                            placeholder = { Text("e.g. Add rich sensory details and suspense") },
                            label = { Text("Custom Editorial Instruction") },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = {
                                    val instruction = customRefineInput.ifEmpty { "Polish grammatical flow" }
                                    viewModel.aiRefineContent(highlighted.ifEmpty { editorValue.text }, instruction)
                                },
                                enabled = !isAiLoading,
                                modifier = Modifier.weight(1f)
                            ) { Text("Polish", fontSize = 11.sp) }

                            Button(
                                onClick = { viewModel.aiGetFeedback(highlighted.ifEmpty { editorValue.text }) },
                                enabled = !isAiLoading,
                                modifier = Modifier.weight(1f)
                            ) { Text("Critique Scene", fontSize = 11.sp) }
                        }
                    }
                }

                // Spell 3: Tone Transmuter
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Filled.Style, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text("Tone Transmuter Style", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        
                        val tonesList = listOf("Minimalist (Hemingway)", "Dark Gothic Storyteller", "Epic Fantasy Bard", "Noir Detective Novelist", "Whimsical Fairy Tale")
                        var dropExpanded by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { dropExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(selectedTone, style = MaterialTheme.typography.bodyMedium)
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = dropExpanded, onDismissRequest = { dropExpanded = false }) {
                                tonesList.forEach { tone ->
                                    DropdownMenuItem(
                                        text = { Text(tone) },
                                        onClick = { selectedTone = tone; dropExpanded = false }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val highlighted = if (editorValue.selection.min != editorValue.selection.max) {
                                    editorValue.text.substring(editorValue.selection.min, editorValue.selection.max)
                                } else ""
                                viewModel.aiRewriteWithTone(highlighted.ifEmpty { editorValue.text }, selectedTone)
                            },
                            enabled = !isAiLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Mutate Tone")
                        }
                    }
                }
            }
        } else {
            // OUTPUT DISPLAY PANEL (Beautiful parchment effect output display)
            Column(modifier = Modifier.fillMaxSize()) {
                if (isAiLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                            Text("Scribing dynamic edits...", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
                        }
                    }
                } else if (aiError != null) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                            Text(aiError!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        }
                    }
                } else if (aiOutput != null) {
                    Column(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onInsertOutput(aiOutput!!) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Input, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Append", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { onReplaceOutput(aiOutput!!) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.FindReplace, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Replace Selected", fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.alpha(0.6f)) {
                            Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(40.dp))
                            Text("Spells request outputs will draft onto parchment sheets directly here.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}
