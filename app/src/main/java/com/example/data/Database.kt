package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Entities ---

@Entity(tableName = "stories")
data class Story(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val genre: String,
    val synopsis: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = Story::class,
            parentColumns = ["id"],
            childColumns = ["storyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["storyId"])]
)
data class Chapter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val storyId: Int,
    val title: String,
    val sortOrder: Int
)

@Entity(
    tableName = "sections",
    foreignKeys = [
        ForeignKey(
            entity = Chapter::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["chapterId"])]
)
data class Section(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chapterId: Int,
    val title: String,
    val content: String,
    val sortOrder: Int,
    val updatedAt: Long = System.currentTimeMillis()
)

// --- DAO ---

@Dao
interface StoryDao {
    // Stories
    @Query("SELECT * FROM stories ORDER BY createdAt DESC")
    fun getAllStories(): Flow<List<Story>>

    @Query("SELECT * FROM stories WHERE id = :id LIMIT 1")
    suspend fun getStoryById(id: Int): Story?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: Story): Long

    @Update
    suspend fun updateStory(story: Story)

    @Delete
    suspend fun deleteStory(story: Story)

    // Chapters
    @Query("SELECT * FROM chapters WHERE storyId = :storyId ORDER BY sortOrder ASC, id ASC")
    fun getChaptersForStory(storyId: Int): Flow<List<Chapter>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: Chapter): Long

    @Update
    suspend fun updateChapter(chapter: Chapter)

    @Delete
    suspend fun deleteChapter(chapter: Chapter)

    // Sections
    @Query("SELECT * FROM sections WHERE chapterId = :chapterId ORDER BY sortOrder ASC, id ASC")
    fun getSectionsForChapter(chapterId: Int): Flow<List<Section>>

    @Query("SELECT s.* FROM sections s INNER JOIN chapters c ON s.chapterId = c.id WHERE c.storyId = :storyId")
    fun getAllSectionsForStory(storyId: Int): Flow<List<Section>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: Section): Long

    @Update
    suspend fun updateSection(section: Section)

    @Delete
    suspend fun deleteSection(section: Section)
}

// --- Database Class ---

@Database(
    entities = [Story::class, Chapter::class, Section::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val storyDao: StoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "musewriter_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
