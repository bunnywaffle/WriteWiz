package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.StoryRepository
import com.example.ui.dashboard.StoryDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.StoryViewModel
import com.example.ui.viewmodel.StoryViewModelFactory
import com.example.ui.workshop.StoryWorkshopScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fully blood-edge transparent drawing
        enableEdgeToEdge()

        // Init local Room DB & Repo
        val database = AppDatabase.getDatabase(this)
        val repository = StoryRepository(database.storyDao)
        val viewModelFactory = StoryViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                // Obtain our Single-Source-of-Truth ViewModel
                val viewModel: StoryViewModel = viewModel(factory = viewModelFactory)

                val stories by viewModel.stories.collectAsState()
                val currentStory by viewModel.currentStory.collectAsState()

                // Register standard Android hardware back key behavior
                if (currentStory != null) {
                    BackHandler {
                        viewModel.selectStory(null)
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    // Transition animations for screen changes
                    AnimatedContent(
                        targetState = currentStory,
                        transitionSpec = {
                            if (targetState != null) {
                                // Slide in from right (Workshop entry)
                                (slideInHorizontally { width -> width } + fadeIn())
                                    .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                            } else {
                                // Slide out to right (Return to Dashboard)
                                (slideInHorizontally { width -> -width } + fadeIn())
                                    .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                            }
                        },
                        label = "ScreenTransition"
                    ) { activeStory ->
                        if (activeStory == null) {
                            StoryDashboardScreen(
                                stories = stories,
                                onSelectStory = { story -> viewModel.selectStory(story) },
                                onCreateStory = { title, genre, synopsis ->
                                    viewModel.createStory(title, genre, synopsis) { savedStory ->
                                        // Auto-open workshop once story is created
                                        viewModel.selectStory(savedStory)
                                    }
                                },
                                onDeleteStory = { story -> viewModel.deleteStory(story) }
                            )
                        } else {
                            StoryWorkshopScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.selectStory(null) }
                            )
                        }
                    }
                }
            }
        }
    }
}
