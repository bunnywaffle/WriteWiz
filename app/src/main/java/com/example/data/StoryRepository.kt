package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StoryRepository(private val dao: StoryDao) {

    val allStories: Flow<List<Story>> = dao.getAllStories()

    suspend fun getStoryById(id: Int): Story? = withContext(Dispatchers.IO) {
        dao.getStoryById(id)
    }

    suspend fun insertStory(story: Story): Int = withContext(Dispatchers.IO) {
        dao.insertStory(story).toInt()
    }

    suspend fun updateStory(story: Story) = withContext(Dispatchers.IO) {
        dao.updateStory(story)
    }

    suspend fun deleteStory(story: Story) = withContext(Dispatchers.IO) {
        dao.deleteStory(story)
    }

    // Chapters
    fun getChaptersForStory(storyId: Int): Flow<List<Chapter>> {
        return dao.getChaptersForStory(storyId)
    }

    suspend fun insertChapter(chapter: Chapter): Int = withContext(Dispatchers.IO) {
        dao.insertChapter(chapter).toInt()
    }

    suspend fun updateChapter(chapter: Chapter) = withContext(Dispatchers.IO) {
        dao.updateChapter(chapter)
    }

    suspend fun deleteChapter(chapter: Chapter) = withContext(Dispatchers.IO) {
        dao.deleteChapter(chapter)
    }

    // Sections
    fun getSectionsForChapter(chapterId: Int): Flow<List<Section>> {
        return dao.getSectionsForChapter(chapterId)
    }

    fun getAllSectionsForStory(storyId: Int): Flow<List<Section>> {
        return dao.getAllSectionsForStory(storyId)
    }

    suspend fun insertSection(section: Section): Int = withContext(Dispatchers.IO) {
        dao.insertSection(section).toInt()
    }

    suspend fun updateSection(section: Section) = withContext(Dispatchers.IO) {
        dao.updateSection(section)
    }

    suspend fun deleteSection(section: Section) = withContext(Dispatchers.IO) {
        dao.deleteSection(section)
    }
}
