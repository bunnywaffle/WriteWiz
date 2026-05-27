package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StoryViewModel(private val repository: StoryRepository) : ViewModel() {

    // --- State Observables ---

    val stories: StateFlow<List<Story>> = repository.allStories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentStory = MutableStateFlow<Story?>(null)
    val currentStory: StateFlow<Story?> = _currentStory.asStateFlow()

    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters.asStateFlow()

    private val _currentChapter = MutableStateFlow<Chapter?>(null)
    val currentChapter: StateFlow<Chapter?> = _currentChapter.asStateFlow()

    private val _sections = MutableStateFlow<List<Section>>(emptyList())
    val sections: StateFlow<List<Section>> = _sections.asStateFlow()

    private val _currentSection = MutableStateFlow<Section?>(null)
    val currentSection: StateFlow<Section?> = _currentSection.asStateFlow()

    // --- AI Assistant State ---

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiOutput = MutableStateFlow<String?>(null)
    val aiOutput: StateFlow<String?> = _aiOutput.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    // --- Flow Observers for Subtopics ---

    init {
        // Automatically load chapters when story changes
        viewModelScope.launch {
            currentStory.collect { story ->
                if (story != null) {
                    repository.getChaptersForStory(story.id).collect { chapterList ->
                        _chapters.value = chapterList
                        // Auto-select first chapter if nothing is selected or previous is gone
                        if (_currentChapter.value == null || !chapterList.any { it.id == _currentChapter.value?.id }) {
                            _currentChapter.value = chapterList.firstOrNull()
                        }
                    }
                } else {
                    _chapters.value = emptyList()
                    _currentChapter.value = null
                }
            }
        }

        // Automatically load sections when current chapter changes
        viewModelScope.launch {
            currentChapter.collect { chapter ->
                if (chapter != null) {
                    repository.getSectionsForChapter(chapter.id).collect { sectionList ->
                        _sections.value = sectionList
                        if (_currentSection.value == null || !sectionList.any { it.id == _currentSection.value?.id }) {
                            _currentSection.value = sectionList.firstOrNull()
                        }
                    }
                } else {
                    _sections.value = emptyList()
                    _currentSection.value = null
                }
            }
        }
    }

    // --- Story Operations ---

    fun selectStory(story: Story?) {
        _currentStory.value = story
        _currentChapter.value = null
        _currentSection.value = null
        clearAiState()
    }

    fun createStory(title: String, genre: String, synopsis: String, onComplete: (Story) -> Unit = {}) {
        viewModelScope.launch {
            val s = Story(title = title, genre = genre, synopsis = synopsis)
            val id = repository.insertStory(s)
            val savedStory = s.copy(id = id)
            
            // Auto create a default First Chapter and Section
            val chapId = repository.insertChapter(Chapter(storyId = savedStory.id, title = "Chapter 1: The Beginning", sortOrder = 1))
            val secId = repository.insertSection(Section(chapterId = chapId, title = "Introduction", content = "# Introduction\n\nWrite your first scene draft here...", sortOrder = 1))
            
            onComplete(savedStory)
        }
    }

    fun updateStory(title: String, genre: String, synopsis: String) {
        val current = _currentStory.value ?: return
        viewModelScope.launch {
            val updated = current.copy(title = title, genre = genre, synopsis = synopsis)
            repository.updateStory(updated)
            _currentStory.value = updated
        }
    }

    fun deleteStory(story: Story) {
        viewModelScope.launch {
            if (_currentStory.value?.id == story.id) {
                selectStory(null)
            }
            repository.deleteStory(story)
        }
    }

    // --- Chapter Operations ---

    fun selectChapter(chapter: Chapter?) {
        _currentChapter.value = chapter
        _currentSection.value = null
        clearAiState()
    }

    fun createChapter(title: String) {
        val story = _currentStory.value ?: return
        viewModelScope.launch {
            val order = (_chapters.value.maxOfOrNull { it.sortOrder } ?: 0) + 1
            val chapId = repository.insertChapter(Chapter(storyId = story.id, title = title, sortOrder = order))
            
            // Create a default placeholder Section inside this chapter
            repository.insertSection(Section(chapterId = chapId, title = "First Scene", content = "# New Scene\n\nBegin chapter details here...", sortOrder = 1))
        }
    }

    fun renameChapter(chapter: Chapter, newTitle: String) {
        viewModelScope.launch {
            val updated = chapter.copy(title = newTitle)
            repository.updateChapter(updated)
            if (_currentChapter.value?.id == chapter.id) {
                _currentChapter.value = updated
            }
        }
    }

    fun deleteChapter(chapter: Chapter) {
        viewModelScope.launch {
            if (_currentChapter.value?.id == chapter.id) {
                _currentChapter.value = null
                _currentSection.value = null
            }
            repository.deleteChapter(chapter)
        }
    }

    // --- Section Operations ---

    fun selectSection(section: Section?) {
        _currentSection.value = section
        clearAiState()
    }

    fun createSection(title: String) {
        val chapter = _currentChapter.value ?: return
        viewModelScope.launch {
            val order = (_sections.value.maxOfOrNull { it.sortOrder } ?: 0) + 1
            val sec = Section(chapterId = chapter.id, title = title, content = "# $title\n\nEnter content here...", sortOrder = order)
            val id = repository.insertSection(sec)
            _currentSection.value = sec.copy(id = id)
        }
    }

    fun updateSectionContent(content: String) {
        val section = _currentSection.value ?: return
        val updated = section.copy(content = content, updatedAt = System.currentTimeMillis())
        _currentSection.value = updated
        viewModelScope.launch {
            repository.updateSection(updated)
        }
    }

    fun renameSection(section: Section, newTitle: String) {
        viewModelScope.launch {
            val updated = section.copy(title = newTitle)
            repository.updateSection(updated)
            if (_currentSection.value?.id == section.id) {
                _currentSection.value = updated
            }
        }
    }

    fun deleteSection(section: Section) {
        viewModelScope.launch {
            if (_currentSection.value?.id == section.id) {
                _currentSection.value = null
            }
            repository.deleteSection(section)
        }
    }

    // --- AI Assistant Features ---

    fun clearAiState() {
        _aiOutput.value = null
        _aiError.value = null
        _isAiLoading.value = false
    }

    /**
     * Assists the writer by continuing the story from current selection or position.
     */
    fun aiContinueWriting(cursorContext: String) {
        val story = _currentStory.value ?: return
        val chapter = _currentChapter.value ?: return
        val section = _currentSection.value ?: return

        _isAiLoading.value = true
        _aiError.value = null
        _aiOutput.value = null

        viewModelScope.launch {
            val sysPrompt = """
                You are a premium AI Novelist and Writing Assistant matching the genre '${story.genre}' of a book titled '${story.title}'.
                The book overview is:
                ${story.synopsis}
                
                You write with highly visual, atmospheric prose, proper pacing, and sensory descriptions. Keep text in markdown.
                Always continue the story seamlessly, directly mimicking the author's tone and returning only the generated continuation. 
                Do not output introductory or conversational text like "Here is a continuation:" or similar. Just return pure prose details!
            """.trimIndent()

            val mainPrompt = """
                Currently, I am writing:
                - Chapter: ${chapter.title}
                - Scene/Section: ${section.title}
                
                The text right before the cursor is:
                [START TEXT]
                $cursorContext
                [END TEXT]
                
                Please generate a continuous flow of 2 to 3 paragraphs (about 150-250 words) to continue writing this scene.
            """.trimIndent()

            val result = GeminiClient.generate(mainPrompt, sysPrompt, temperature = 0.8)
            if (result.startsWith("API Error:") || result.startsWith("Error:")) {
                _aiError.value = result
            } else {
                _aiOutput.value = result
            }
            _isAiLoading.value = false
        }
    }

    /**
     * Refines/polishes the selected write block with precise instructions (e.g., More suspense, sensory depth).
     */
    fun aiRefineContent(selectedText: String, instruction: String) {
        val story = _currentStory.value ?: return

        _isAiLoading.value = true
        _aiError.value = null
        _aiOutput.value = null

        viewModelScope.launch {
            val sysPrompt = """
                You are an expert story editor. Refine the given draft segment based on the author's request.
                Adhere strictly to the requested tone/improvement instruction. Avoid adding conversational fluff in the output.
                Return ONLY the rewritten and formatted segment. Keep or enhance the rich markdown styling.
            """.trimIndent()

            val mainPrompt = """
                Story Details (Genre: ${story.genre}, Synopsis: ${story.synopsis})
                
                Segment to refine:
                [START SEGMENT]
                $selectedText
                [END SEGMENT]
                
                Instruction: $instruction
                
                Please rewrite this segment to fulfill the instruction perfectly.
            """.trimIndent()

            val result = GeminiClient.generate(mainPrompt, sysPrompt, temperature = 0.7)
            if (result.startsWith("API Error:") || result.startsWith("Error:")) {
                _aiError.value = result
            } else {
                _aiOutput.value = result
            }
            _isAiLoading.value = false
        }
    }

    /**
     * Generates active section outline beats.
     */
    fun aiGenerateOutline(sectionTitle: String) {
        val story = _currentStory.value ?: return

        _isAiLoading.value = true
        _aiError.value = null
        _aiOutput.value = null

        viewModelScope.launch {
            val sysPrompt = """
                You are a creative Plot Architect. Generate beat-by-beat plot outlines for dynamic novel chapters.
                Provide details in clearly structured markdown with concrete plot points, subtext, conflict, and character emotions.
                Do not output any introductory or conversation text. Start directly with the outline list.
            """.trimIndent()

            val mainPrompt = """
                Book Title: '${story.title}' (Genre: ${story.genre})
                Overview: ${story.synopsis}
                
                Please generate a beat-by-beat outline of 5-6 dramatic details or plot cards for the scene titled '$sectionTitle'.
            """.trimIndent()

            val result = GeminiClient.generate(mainPrompt, sysPrompt, temperature = 0.85)
            if (result.startsWith("API Error:") || result.startsWith("Error:")) {
                _aiError.value = result
            } else {
                _aiOutput.value = result
            }
            _isAiLoading.value = false
        }
    }

    /**
     * Rewrites selected text with a specific designated style/tone.
     */
    fun aiRewriteWithTone(selectedText: String, tone: String) {
        _isAiLoading.value = true
        _aiError.value = null
        _aiOutput.value = null

        viewModelScope.launch {
            val sysPrompt = "You are a master stylist. Rewrite the provided draft block into the styled prose requested by the author."
            val mainPrompt = """
                Please rewrite the following text in the tone of: '$tone'.
                Ensure you keep the exact semantic plot details, but fully transform the syntax, style, vocabulary, and pacing.
                Return ONLY the rewritten prose segment without extra remarks.
                
                [TEXT]
                $selectedText
                [/TEXT]
            """.trimIndent()

            val result = GeminiClient.generate(mainPrompt, sysPrompt, temperature = 0.75)
            if (result.startsWith("API Error:") || result.startsWith("Error:")) {
                _aiError.value = result
            } else {
                _aiOutput.value = result
            }
            _isAiLoading.value = false
        }
    }

    /**
     * Critiques and provides feedback on pacing, descriptive depth, etc.
     */
    fun aiGetFeedback(content: String) {
        _isAiLoading.value = true
        _aiError.value = null
        _aiOutput.value = null

        viewModelScope.launch {
            val sysPrompt = """
                You are an objective, sharp and helpful literary editor. Analyze the provided draft scene block and deliver actionable suggestions.
                Cover aspects like "Pacing & Tension", "Show vs. Tell", "Sensory Details", and "Actionable Tip".
                Reply in concise, bulleted markdown sections. Avoid excessive praise; offer constructive feedback.
            """.trimIndent()

            val mainPrompt = """
                Please analyze and critique this passage:
                
                $content
            """.trimIndent()

            val result = GeminiClient.generate(mainPrompt, sysPrompt, temperature = 0.6)
            if (result.startsWith("API Error:") || result.startsWith("Error:")) {
                _aiError.value = result
            } else {
                _aiOutput.value = result
            }
            _isAiLoading.value = false
        }
    }
}

// --- Factory for Creating ViewModel ---

class StoryViewModelFactory(private val repository: StoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
