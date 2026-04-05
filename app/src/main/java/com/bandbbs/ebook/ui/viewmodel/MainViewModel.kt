package com.bandbbs.ebook.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.bandbbs.ebook.database.AppDatabase
import com.bandbbs.ebook.database.BookmarkEntity
import com.bandbbs.ebook.database.ChapterInfo

import com.bandbbs.ebook.ui.model.Book
import com.bandbbs.ebook.ui.model.ChapterEditContent
import com.bandbbs.ebook.ui.model.ChapterSegment
import com.bandbbs.ebook.ui.viewmodel.handlers.CategoryHandler

import com.bandbbs.ebook.ui.viewmodel.handlers.ImportHandler
import com.bandbbs.ebook.ui.viewmodel.handlers.LibraryHandler

import com.bandbbs.ebook.utils.BookInfoParser
import com.bandbbs.ebook.utils.BookmarkManager
import com.bandbbs.ebook.utils.ChapterContentManager
import com.bandbbs.ebook.utils.DataBackupManager
import com.bandbbs.ebook.utils.EpubParser
import com.bandbbs.ebook.utils.NvbParser
import com.bandbbs.ebook.utils.ReadingTimeStorage
import com.bandbbs.ebook.utils.VersionChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File



data class GlobalLoadingState(
    val isLoading: Boolean = false,
    val message: String = ""
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val booksDir = File(application.filesDir, "books").apply { mkdirs() }
    private val db = AppDatabase.getDatabase(application)
    private val prefs: SharedPreferences =
        application.getSharedPreferences("ebook_prefs", Context.MODE_PRIVATE)
    private val readerPrefs: SharedPreferences =
        application.getSharedPreferences("chapter_reader_prefs", Context.MODE_PRIVATE)
    private val FIRST_SYNC_CONFIRMED_KEY = "first_sync_confirmed"
    private val FIRST_SYNC_READING_DATA_CONFIRMED_KEY = "first_sync_reading_data_confirmed"



    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books = _books.asStateFlow()

    private val _recentBook = MutableStateFlow<Book?>(null)
    val recentBook = _recentBook.asStateFlow()

    private val _recentUpdatedBook = MutableStateFlow<Book?>(null)
    val recentUpdatedBook = _recentUpdatedBook.asStateFlow()


    private val _expandedBookPath = MutableStateFlow<String?>(null)
    val expandedBookPath = _expandedBookPath.asStateFlow()

    private val _expandedCategories = MutableStateFlow<Set<String>>(emptySet())
    val expandedCategories = _expandedCategories.asStateFlow()



    private val _importState = MutableStateFlow<ImportState?>(null)
    val importState = _importState.asStateFlow()

    private val _importingState = MutableStateFlow<ImportingState?>(null)
    val importingState = _importingState.asStateFlow()

    private val _importReportState = MutableStateFlow<ImportReportState?>(null)
    val importReportState = _importReportState.asStateFlow()

    private val _selectedBookForChapters = MutableStateFlow<Book?>(null)
    val selectedBookForChapters = _selectedBookForChapters.asStateFlow()

    private val _chaptersForSelectedBook = MutableStateFlow<List<ChapterInfo>>(emptyList())
    val chaptersForSelectedBook = _chaptersForSelectedBook.asStateFlow()

    private val _chapterToPreview =
        MutableStateFlow<com.bandbbs.ebook.ui.model.ChapterWithContent?>(null)
    val chapterToPreview = _chapterToPreview.asStateFlow()

    private val _chaptersForPreview = MutableStateFlow<List<ChapterInfo>>(emptyList())
    val chaptersForPreview = _chaptersForPreview.asStateFlow()

    private val _chapterEditorContent = MutableStateFlow<ChapterEditContent?>(null)
    val chapterEditorContent = _chapterEditorContent.asStateFlow()

    private val _bookToDelete = MutableStateFlow<Book?>(null)
    val bookToDelete = _bookToDelete.asStateFlow()

    private val _booksToDelete = MutableStateFlow<List<Book>>(emptyList())
    val booksToDelete = _booksToDelete.asStateFlow()



    private val _overwriteConfirmState = MutableStateFlow<OverwriteConfirmState?>(null)
    val overwriteConfirmState = _overwriteConfirmState.asStateFlow()

    private val _bookForCoverImport = MutableStateFlow<Book?>(null)
    val bookForCoverImport = _bookForCoverImport.asStateFlow()



    private val _categoryState = MutableStateFlow<CategoryState?>(null)
    val categoryState = _categoryState.asStateFlow()



    private val _editBookInfoState = MutableStateFlow<EditBookInfoState?>(null)
    val editBookInfoState = _editBookInfoState.asStateFlow()



    private val _syncResultState = MutableStateFlow<SyncResultState?>(null)
    val syncResultState = _syncResultState.asStateFlow()

    private var syncReadingDataJob: Job? = null



    private val _updateCheckState = MutableStateFlow(UpdateCheckState())
    val updateCheckState = _updateCheckState.asStateFlow()





    private val _backupRestoreState = MutableStateFlow<BackupRestoreResult?>(null)
    val backupRestoreState = _backupRestoreState.asStateFlow()



    private val _globalLoadingState = MutableStateFlow(GlobalLoadingState())
    val globalLoadingState = _globalLoadingState.asStateFlow()

    private val SHOW_RECENT_IMPORT_KEY = "show_recent_import"
    private val SHOW_RECENT_UPDATE_KEY = "show_recent_update"
    private val SHOW_SEARCH_BAR_KEY = "show_search_bar"
    private val THEME_MODE_KEY = "theme_mode"
    private val QUICK_EDIT_CATEGORY_KEY = "quick_edit_category"
    private val QUICK_RENAME_CATEGORY_KEY = "quick_rename_category"
    private val LAST_SPLIT_METHOD_KEY = "last_split_method"


    private val _showRecentImport = MutableStateFlow(prefs.getBoolean(SHOW_RECENT_IMPORT_KEY, true))
    val showRecentImport = _showRecentImport.asStateFlow()

    private val _showRecentUpdate = MutableStateFlow(prefs.getBoolean(SHOW_RECENT_UPDATE_KEY, true))
    val showRecentUpdate = _showRecentUpdate.asStateFlow()

    private val _showSearchBar = MutableStateFlow(prefs.getBoolean(SHOW_SEARCH_BAR_KEY, true))
    val showSearchBar = _showSearchBar.asStateFlow()

    private val _quickEditCategoryEnabled =
        MutableStateFlow(prefs.getBoolean(QUICK_EDIT_CATEGORY_KEY, false))
    val quickEditCategoryEnabled = _quickEditCategoryEnabled.asStateFlow()

    private val _quickRenameCategoryEnabled =
        MutableStateFlow(prefs.getBoolean(QUICK_RENAME_CATEGORY_KEY, false))
    val quickRenameCategoryEnabled = _quickRenameCategoryEnabled.asStateFlow()

    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode = _isMultiSelectMode.asStateFlow()

    private val _selectedBooks = MutableStateFlow<Set<String>>(emptySet())
    val selectedBooks = _selectedBooks.asStateFlow()

    enum class ThemeMode {
        LIGHT, DARK, SYSTEM
    }

    private val _themeMode = MutableStateFlow(
        ThemeMode.valueOf(
            prefs.getString(THEME_MODE_KEY, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        )
    )
    val themeMode = _themeMode.asStateFlow()

    private val categoryHandler = CategoryHandler(
        prefs = prefs,
        db = db,
        scope = viewModelScope,
        categoryState = _categoryState,
        importState = _importState,
        onBooksChanged = { loadBooks() }
    )

    private val importHandler = ImportHandler(
        application = application,
        db = db,
        booksDir = booksDir,
        scope = viewModelScope,
        booksState = _books,
        importState = _importState,
        importingState = _importingState,
        importReportState = _importReportState,
        overwriteConfirmState = _overwriteConfirmState,
        onBooksChanged = { loadBooks() }
    )

    private val libraryHandler = LibraryHandler(
        application = application,
        db = db,
        booksDir = booksDir,
        scope = viewModelScope,
        bookToDelete = _bookToDelete,
        selectedBookForChapters = _selectedBookForChapters,
        chaptersForSelectedBook = _chaptersForSelectedBook,
        chapterToPreview = _chapterToPreview,
        chaptersForPreview = _chaptersForPreview,
        bookForCoverImport = _bookForCoverImport,
        chapterEditorContent = _chapterEditorContent,
        onBooksChanged = { loadBooks() }
    )

    init {
        loadBooks()
    }




    fun setExpandedBook(path: String?) {
        _expandedBookPath.value = path
    }

    fun toggleCategoryExpansion(category: String) {
        val current = _expandedCategories.value
        if (current.contains(category)) {
            _expandedCategories.value = current - category
        } else {
            _expandedCategories.value = current + category
        }
    }

    fun getCategories(): List<String> = categoryHandler.getCategories()

    fun showCategorySelector(book: Book? = null) = categoryHandler.showCategorySelector(book)

    fun showCategorySelectorForEditBookInfo(
        selectedCategory: String?,
        onCategorySelected: (String?) -> Unit
    ) {
        categoryHandler.showCategorySelectorForEdit(selectedCategory, onCategorySelected)
    }

    fun createCategory(categoryName: String) = categoryHandler.createCategory(categoryName)

    fun deleteCategory(categoryName: String) = categoryHandler.deleteCategory(categoryName)

    fun selectCategory(category: String?) = categoryHandler.selectCategory(category)

    fun dismissCategorySelector() = categoryHandler.dismissCategorySelector()

    fun enterMultiSelectMode() {
        _isMultiSelectMode.value = true
        _selectedBooks.value = emptySet()
    }

    fun exitMultiSelectMode() {
        _isMultiSelectMode.value = false
        _selectedBooks.value = emptySet()
    }

    fun selectBook(bookPath: String) {
        val current = _selectedBooks.value.toMutableSet()
        if (current.contains(bookPath)) {
            current.remove(bookPath)
        } else {
            current.add(bookPath)
        }
        _selectedBooks.value = current
    }

    fun requestDeleteSelectedBooks() {
        val selectedPaths = _selectedBooks.value
        if (selectedPaths.isEmpty()) return

        val booksToDelete = _books.value.filter { it.path in selectedPaths }
        if (booksToDelete.isEmpty()) return

        _booksToDelete.value = booksToDelete
    }

    fun cancelDeleteSelectedBooks() {
        _booksToDelete.value = emptyList()
    }

    fun confirmDeleteSelectedBooks() {
        val booksToDelete = _booksToDelete.value
        if (booksToDelete.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            booksToDelete.forEach { book ->
                File(book.path).delete()
                val bookEntity = db.bookDao().getBookByPath(book.path)
                if (bookEntity != null) {
                    val context = application.applicationContext
                    ChapterContentManager.deleteBookChapters(context, bookEntity.id)
                    db.chapterDao().deleteChaptersByBookId(bookEntity.id)
                    db.bookDao().delete(bookEntity)
                }
            }
            withContext(Dispatchers.Main) {
                _booksToDelete.value = emptyList()
                loadBooks()
                exitMultiSelectMode()
            }
        }
    }



    fun startImport(uri: Uri) = importHandler.startImport(uri)

    fun startImportBatch(uris: List<Uri>) = importHandler.startImportBatch(uris)

    fun cancelImport() = importHandler.cancelImport()

    fun dismissImportProgress() {
        _importingState.value = null
    }

    fun confirmImport(
        bookName: String,
        splitMethod: String,
        noSplit: Boolean,
        wordsPerChapter: Int,
        selectedCategory: String? = null,
        enableChapterMerge: Boolean = false,
        mergeMinWords: Int = 500,
        enableChapterRename: Boolean = false,
        renamePattern: String = "",
        customRegex: String = ""
    ) = importHandler.confirmImport(
        bookName,
        splitMethod,
        noSplit,
        wordsPerChapter,
        selectedCategory,
        enableChapterMerge,
        mergeMinWords,
        enableChapterRename,
        renamePattern,
        customRegex
    )

    fun cancelOverwriteConfirm() = importHandler.cancelOverwriteConfirm()

    fun confirmOverwrite() = importHandler.confirmOverwrite()

    fun dismissImportReport() = importHandler.dismissImportReport()

    fun requestDeleteBook(book: Book) = libraryHandler.requestDeleteBook(book)

    fun confirmDeleteBook() = libraryHandler.confirmDeleteBook()

    fun cancelDeleteBook() = libraryHandler.cancelDeleteBook()



    fun showChapterList(book: Book) = libraryHandler.showChapterList(book)

    fun closeChapterList() = libraryHandler.closeChapterList()

    fun showChapterPreview(chapterId: Int) = libraryHandler.showChapterPreview(chapterId)

    fun continueReading(book: Book) = libraryHandler.continueReading(book)

    fun closeChapterPreview() {
        libraryHandler.closeChapterPreview()
        loadBooks()
    }

    fun renameChapter(chapterId: Int, newTitle: String) =
        libraryHandler.renameChapter(chapterId, newTitle)

    fun moveChapter(chapterId: Int, direction: Int) =
        libraryHandler.moveChapter(chapterId, direction)

    fun reorderChapter(chapterId: Int, targetIndex: Int) =
        libraryHandler.reorderChapter(chapterId, targetIndex)

    fun openChapterEditor(chapterId: Int) = libraryHandler.openChapterEditor(chapterId)

    fun closeChapterEditor() = libraryHandler.closeChapterEditor()

    fun saveChapterContent(chapterId: Int, title: String, content: String) =
        libraryHandler.saveChapterContent(chapterId, title, content)

    suspend fun loadChapterContent(chapterId: Int): String {
        return withContext(Dispatchers.IO) {
            val chapter = db.chapterDao().getChapterById(chapterId)
            if (chapter != null) {
                ChapterContentManager.readChapterContent(chapter.contentFilePath)
            } else {
                ""
            }
        }
    }

    fun addChapter(insertIndex: Int, title: String, content: String) =
        libraryHandler.addChapter(insertIndex, title, content)

    fun batchRenameChapters(
        chapterIds: List<Int>,
        prefix: String,
        suffix: String,
        startNumber: Int,
        padding: Int
    ) = libraryHandler.batchRenameChapters(chapterIds, prefix, suffix, startNumber, padding)

    fun mergeChapters(chapterIds: List<Int>, mergedTitle: String, insertBlankLine: Boolean) =
        libraryHandler.mergeChapters(chapterIds, mergedTitle, insertBlankLine)

    fun splitChapter(chapterId: Int, segments: List<ChapterSegment>) =
        libraryHandler.splitChapter(chapterId, segments)

    fun requestImportCover(book: Book) = libraryHandler.requestImportCover(book)

    fun cancelImportCover() = libraryHandler.cancelImportCover()

    fun importCoverForBook(uri: Uri) = libraryHandler.importCoverForBook(uri)

    fun showEditBookInfo(book: Book) {
        viewModelScope.launch(Dispatchers.IO) {
            val bookEntity = db.bookDao().getBookByPath(book.path)
            if (bookEntity != null) {
                _editBookInfoState.value = EditBookInfoState(bookEntity, isResyncing = false)
            }
        }
    }

    fun dismissEditBookInfo() {
        _editBookInfoState.value = null
    }

    fun saveBookInfo(bookEntity: com.bandbbs.ebook.database.BookEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.bookDao().update(bookEntity)
            loadBooks()
            withContext(Dispatchers.Main) {
                _editBookInfoState.value = null
            }
        }
    }

    suspend fun saveBookInfoWithoutDismiss(bookEntity: com.bandbbs.ebook.database.BookEntity) {
        withContext(Dispatchers.IO) {
            db.bookDao().update(bookEntity)
            loadBooks()
        }

        withContext(Dispatchers.Main) {
            _editBookInfoState.value?.let { currentState ->
                _editBookInfoState.value = currentState.copy(book = bookEntity)
            }
        }
    }

    fun resyncBookCategory(book: Book) {
        viewModelScope.launch(Dispatchers.IO) {
            val bookEntity = db.bookDao().getBookByPath(book.path)
            if (bookEntity != null) {
                try {
                    val context = getApplication<Application>().applicationContext
                    val fileUri = Uri.fromFile(File(book.path))

                    when (bookEntity.format) {
                        "nvb" -> {
                            val nvbBook = NvbParser.parse(context, fileUri)
                            val updatedEntity = bookEntity.copy(
                                category = nvbBook.metadata.category,
                                localCategory = bookEntity.localCategory
                            )
                            db.bookDao().update(updatedEntity)
                        }

                        "epub" -> {
                            EpubParser.parse(context, fileUri)


                        }
                    }
                    loadBooks()
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to resync book category", e)
                }
            }
        }
    }

    fun resyncBookInfo(book: Book) {
        _globalLoadingState.value =
            GlobalLoadingState(isLoading = true, message = "正在同步书籍信息...")
        viewModelScope.launch(Dispatchers.IO) {

            _editBookInfoState.value?.let { currentState ->
                _editBookInfoState.value = currentState.copy(isResyncing = true)
            }

            val bookEntity = db.bookDao().getBookByPath(book.path)
            if (bookEntity != null) {
                try {
                    val context = getApplication<Application>().applicationContext
                    val fileUri = Uri.fromFile(File(book.path))
                    var updatedEntity: com.bandbbs.ebook.database.BookEntity? = null

                    when (bookEntity.format) {
                        "nvb" -> {
                            val nvbBook = NvbParser.parse(context, fileUri)
                            updatedEntity = bookEntity.copy(
                                author = nvbBook.metadata.author,
                                summary = nvbBook.metadata.summary,
                                bookStatus = nvbBook.metadata.bookStatus,
                                category = nvbBook.metadata.category
                            )
                            db.bookDao().update(updatedEntity)
                        }

                        "epub" -> {
                            val epubBook = EpubParser.parse(context, fileUri)
                            updatedEntity = bookEntity.copy(
                                author = epubBook.author
                            )
                            db.bookDao().update(updatedEntity)
                        }

                        "txt" -> {

                            val chapters = db.chapterDao().getChapterInfoForBook(bookEntity.id)
                            if (chapters.isNotEmpty() &&
                                (chapters[0].name == "简介" || chapters[0].name == "介绍")
                            ) {
                                val chapter = db.chapterDao().getChapterById(chapters[0].id)
                                if (chapter != null) {
                                    val content =
                                        ChapterContentManager.readChapterContent(chapter.contentFilePath)
                                    val parsedInfo =
                                        BookInfoParser.parseIntroductionContent(content)
                                    if (parsedInfo != null) {
                                        updatedEntity = bookEntity.copy(
                                            author = parsedInfo.author ?: bookEntity.author,
                                            summary = parsedInfo.summary ?: bookEntity.summary,
                                            bookStatus = parsedInfo.status ?: bookEntity.bookStatus,
                                            category = parsedInfo.tags ?: bookEntity.category
                                        )
                                        db.bookDao().update(updatedEntity)
                                    }
                                }
                            }
                        }

                        "docx" -> {
                            val chapters = db.chapterDao().getChapterInfoForBook(bookEntity.id)
                            if (chapters.isNotEmpty() &&
                                (chapters[0].name == "简介" || chapters[0].name == "介绍")
                            ) {
                                val chapter = db.chapterDao().getChapterById(chapters[0].id)
                                if (chapter != null) {
                                    val content =
                                        ChapterContentManager.readChapterContent(chapter.contentFilePath)
                                    val parsedInfo =
                                        BookInfoParser.parseIntroductionContent(content)
                                    if (parsedInfo != null) {
                                        updatedEntity = bookEntity.copy(
                                            author = parsedInfo.author ?: bookEntity.author,
                                            summary = parsedInfo.summary ?: bookEntity.summary,
                                            bookStatus = parsedInfo.status ?: bookEntity.bookStatus,
                                            category = parsedInfo.tags ?: bookEntity.category
                                        )
                                        db.bookDao().update(updatedEntity)
                                    }
                                }
                            }
                        }
                    }


                    if (updatedEntity != null) {
                        withContext(Dispatchers.Main) {
                            _editBookInfoState.value =
                                EditBookInfoState(updatedEntity, isResyncing = false)
                        }
                    } else {

                        withContext(Dispatchers.Main) {
                            _editBookInfoState.value?.let { currentState ->
                                _editBookInfoState.value = currentState.copy(isResyncing = false)
                            }
                        }
                    }

                    loadBooks()
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to resync book info", e)

                    withContext(Dispatchers.Main) {
                        _editBookInfoState.value?.let { currentState ->
                            _editBookInfoState.value = currentState.copy(isResyncing = false)
                        }
                    }
                }
            } else {

                withContext(Dispatchers.Main) {
                    _editBookInfoState.value?.let { currentState ->
                        _editBookInfoState.value = currentState.copy(isResyncing = false)
                    }
                }
            }
            withContext(Dispatchers.Main) {
                _globalLoadingState.value = GlobalLoadingState(isLoading = false)
            }
        }
    }





    fun setSyncModeAndStart(mode: SyncMode) {
        setSyncModesAndStart(mode, mode)
    }

    fun setSyncModesAndStart(
        progressMode: SyncMode,
        readingTimeMode: SyncMode,
        bookmarkMode: SyncMode = SyncMode.AUTO
    ) {
        Log.d(
            "MainViewModel",
            "setSyncModesAndStart() called with progressMode: $progressMode, readingTimeMode: $readingTimeMode, bookmarkMode: $bookmarkMode"
        )
        syncReadingDataJob?.cancel()

        syncReadingDataJob = viewModelScope.launch(Dispatchers.IO) {
            try {

                val currentProgressMode = progressMode
                val currentReadingTimeMode = readingTimeMode
                val currentBookmarkMode = bookmarkMode

                withContext(Dispatchers.Main) {
                    _syncReadingDataState.value = _syncReadingDataState.value.copy(
                        showModeDialog = false,
                        progressSyncMode = currentProgressMode,
                        readingTimeSyncMode = currentReadingTimeMode,
                        bookmarkSyncMode = currentBookmarkMode
                    )
                }

                val allBooks = _books.value
                Log.d("MainViewModel", "Starting sync for ${allBooks.size} books")
                if (allBooks.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _syncReadingDataState.value = SyncReadingDataState(
                            isSyncing = false,
                            statusText = "没有书籍需要同步",
                            progress = 1f
                        )
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    _syncReadingDataState.value = _syncReadingDataState.value.copy(
                        isSyncing = true,
                        statusText = "开始同步阅读数据...",
                        progress = 0f,
                        totalBooks = allBooks.size,
                        syncedBooks = 0
                    )
                }

                val fileConn = connectionHandler.getFileConnection()
                var syncedCount = 0
                val changedBooks = mutableListOf<String>()
                val failedBooks = mutableMapOf<String, String>()

                for ((index, book) in allBooks.withIndex()) {

                    if (!coroutineContext.isActive) {
                        withContext(Dispatchers.Main) {
                            _syncReadingDataState.value = SyncReadingDataState(
                                isSyncing = false,
                                statusText = "同步已取消",
                                progress = 0f
                            )
                        }
                        return@launch
                    }

                    withContext(Dispatchers.Main) {
                        _syncReadingDataState.value = _syncReadingDataState.value.copy(
                            currentBook = book.name,
                            progress = index.toFloat() / allBooks.size
                        )
                    }

                    val bookStatus = try {
                        fileConn.getBookStatus(book.name)
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Failed to get book status for ${book.name}", e)
                        failedBooks[book.name] = "获取书籍状态失败: ${e.message ?: "未知错误"}"
                        withContext(Dispatchers.Main) {
                            val currentFailed =
                                _syncReadingDataState.value.failedBooks.toMutableMap()
                            currentFailed[book.name] =
                                "获取书籍状态失败: ${e.message ?: "未知错误"}"
                            _syncReadingDataState.value = _syncReadingDataState.value.copy(
                                failedBooks = currentFailed
                            )
                        }
                        continue
                    }

                    val bookExistsOnBand =
                        bookStatus.syncedChapters.isNotEmpty() || bookStatus.hasCover
                    if (!bookExistsOnBand) {
                        Log.d(
                            "MainViewModel",
                            "Book ${book.name} does not exist on band: syncedChapters=${bookStatus.syncedChapters.size}, hasCover=${bookStatus.hasCover}"
                        )
                        failedBooks[book.name] = "手环端不存在"
                        withContext(Dispatchers.Main) {
                            val currentFailed =
                                _syncReadingDataState.value.failedBooks.toMutableMap()
                            currentFailed[book.name] = "手环端不存在"
                            _syncReadingDataState.value = _syncReadingDataState.value.copy(
                                failedBooks = currentFailed
                            )
                        }
                        continue
                    }

                    Log.d(
                        "MainViewModel",
                        "Book ${book.name} exists on band: syncedChapters=${bookStatus.syncedChapters.size}, hasCover=${bookStatus.hasCover}"
                    )

                    try {
                        Log.d("MainViewModel", "Syncing reading data for book: ${book.name}")

                        val bandReadingData: com.bandbbs.ebook.logic.ReadingDataResult? = try {
                            val data = fileConn.getReadingData(book.name)
                            Log.d(
                                "MainViewModel",
                                "Got reading data from band for ${book.name}: progress=${data.progress != null}, readingTime=${data.readingTime != null}"
                            )
                            data
                        } catch (e: Exception) {
                            Log.e(
                                "MainViewModel",
                                "Failed to get reading data from band for ${book.name}",
                                e
                            )
                            null
                        }


                        val phoneProgress = getPhoneReadingProgress(book)
                        Log.d(
                            "MainViewModel",
                            "Phone progress for ${book.name}: ${phoneProgress != null}"
                        )
                        var bandProgress: Map<String, Any>? = null
                        if (bandReadingData != null) {
                            try {
                                if (bandReadingData.progress != null) {
                                    val progressMap = JSONObject(bandReadingData.progress)
                                    val tempMap = mutableMapOf<String, Any>()
                                    val keys = progressMap.keys()
                                    while (keys.hasNext()) {
                                        val key = keys.next()
                                        val value = progressMap.get(key)

                                        if (key == "chapterIndex") {
                                            when {
                                                value == JSONObject.NULL -> {
                                                    continue
                                                }

                                                value is Int -> tempMap[key] = value
                                                value is Long -> tempMap[key] = value.toInt()
                                                value is Double -> tempMap[key] = value.toInt()
                                                value is String -> {
                                                    try {
                                                        val intValue = value.toInt()
                                                        if (intValue >= 0) {
                                                            tempMap[key] = intValue
                                                        }
                                                    } catch (e: Exception) {
                                                    }
                                                }

                                                else -> {
                                                    try {
                                                        val intValue = (value as? Number)?.toInt()
                                                        if (intValue != null && intValue >= 0) {
                                                            tempMap[key] = intValue
                                                        }
                                                    } catch (e: Exception) {
                                                    }
                                                }
                                            }
                                        } else {
                                            tempMap[key] = when (value) {
                                                is JSONObject -> value.toString()
                                                is org.json.JSONArray -> value.toString()
                                                is Boolean -> value
                                                is Int -> value
                                                is Long -> value
                                                is Double -> value
                                                is String -> value
                                                else -> value.toString()
                                            }
                                        }
                                    }

                                    if (tempMap.containsKey("chapterIndex")) {
                                        bandProgress = tempMap
                                    } else {
                                        Log.d(
                                            "MainViewModel",
                                            "Band progress has no valid chapterIndex, ignoring"
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("MainViewModel", "Failed to parse band reading data", e)
                            }
                        }


                        val phoneReadingTime = getPhoneReadingTime(book.name)
                        Log.d(
                            "MainViewModel",
                            "Phone reading time for ${book.name}: ${if (phoneReadingTime != null) "exists (totalSeconds=${phoneReadingTime["totalSeconds"]})" else "null"}"
                        )

                        var bandReadingTime: Map<String, Any>? = null
                        if (bandReadingData != null && bandReadingData.readingTime != null) {
                            try {
                                Log.d("MainViewModel", "Parsing band reading time for ${book.name}")
                                val readingTimeMap =
                                    JSONObject(bandReadingData.readingTime)
                                val tempMap = mutableMapOf<String, Any>()
                                val keys = readingTimeMap.keys()
                                while (keys.hasNext()) {
                                    val key = keys.next()
                                    val value = readingTimeMap.get(key)
                                    tempMap[key] = when (value) {
                                        is JSONObject -> {
                                            val sessionMap = mutableMapOf<String, Any>()
                                            val sessionKeys = value.keys()
                                            while (sessionKeys.hasNext()) {
                                                val sessionKey = sessionKeys.next()
                                                sessionMap[sessionKey] = value.get(sessionKey)
                                            }
                                            sessionMap
                                        }

                                        is org.json.JSONArray -> {
                                            val sessionList = mutableListOf<Any>()
                                            for (i in 0 until value.length()) {
                                                val sessionObj = value.getJSONObject(i)
                                                val sessionMap = mutableMapOf<String, Any>()
                                                val sessionKeys = sessionObj.keys()
                                                while (sessionKeys.hasNext()) {
                                                    val sessionKey = sessionKeys.next()
                                                    val sessionValue = sessionObj.get(sessionKey)
                                                    sessionMap[sessionKey] = when (sessionValue) {
                                                        is JSONObject -> sessionValue.toString()
                                                        is org.json.JSONArray -> sessionValue.toString()
                                                        is Boolean -> sessionValue
                                                        is Int -> sessionValue
                                                        is Long -> sessionValue
                                                        is Double -> sessionValue
                                                        is String -> sessionValue
                                                        else -> sessionValue.toString()
                                                    }
                                                }
                                                sessionList.add(sessionMap)
                                            }
                                            sessionList
                                        }

                                        is Boolean -> value
                                        is Int -> value
                                        is Long -> value
                                        is Double -> value
                                        is String -> value
                                        else -> value.toString()
                                    }
                                }
                                bandReadingTime = tempMap
                                val sessionsSize =
                                    when (val sessions = bandReadingTime["sessions"]) {
                                        is List<*> -> sessions.size
                                        else -> 0
                                    }
                                Log.d(
                                    "MainViewModel",
                                    "Parsed band reading time for ${book.name}: totalSeconds=${bandReadingTime["totalSeconds"]}, sessions=$sessionsSize"
                                )
                            } catch (e: Exception) {
                                Log.e(
                                    "MainViewModel",
                                    "Failed to parse band reading time for ${book.name}",
                                    e
                                )
                            }
                        } else {
                            Log.d("MainViewModel", "No band reading time data for ${book.name}")
                        }


                        Log.d(
                            "MainViewModel",
                            "Merging data for ${book.name} with progressMode: $currentProgressMode, readingTimeMode: $currentReadingTimeMode"
                        )


                        val finalProgress = when (currentProgressMode) {
                            SyncMode.AUTO -> {
                                val merged = mergeProgress(phoneProgress, bandProgress)
                                Log.d(
                                    "MainViewModel",
                                    "Auto merged progress for ${book.name}: ${merged != null}"
                                )
                                merged
                            }

                            SyncMode.BAND_ONLY -> {
                                Log.d("MainViewModel", "Using band progress for ${book.name}")
                                bandProgress
                            }

                            SyncMode.PHONE_ONLY -> {
                                Log.d("MainViewModel", "Using phone progress for ${book.name}")
                                phoneProgress
                            }
                        }

                        val finalReadingTime = when (currentReadingTimeMode) {
                            SyncMode.AUTO -> {
                                val merged = mergeReadingTime(phoneReadingTime, bandReadingTime)
                                Log.d(
                                    "MainViewModel",
                                    "Auto merged reading time for ${book.name}: ${if (merged != null) "totalSeconds=${merged["totalSeconds"]}, sessions=${(merged["sessions"] as? List<*>)?.size ?: 0}" else "null"}"
                                )
                                merged
                            }

                            SyncMode.BAND_ONLY -> {
                                Log.d("MainViewModel", "Using band reading time for ${book.name}")
                                bandReadingTime
                            }

                            SyncMode.PHONE_ONLY -> {
                                Log.d("MainViewModel", "Using phone reading time for ${book.name}")
                                phoneReadingTime
                            }
                        }

                        val hasProgressChange = when (currentProgressMode) {
                            SyncMode.AUTO -> finalProgress != phoneProgress
                            SyncMode.BAND_ONLY -> bandProgress != null
                            SyncMode.PHONE_ONLY -> false
                        }

                        val hasReadingTimeChange = when (currentReadingTimeMode) {
                            SyncMode.AUTO -> {
                                val phoneTotal =
                                    (phoneReadingTime?.get("totalSeconds") as? Number)?.toLong()
                                        ?: 0L
                                val bandTotal =
                                    (bandReadingTime?.get("totalSeconds") as? Number)?.toLong()
                                        ?: 0L
                                val finalTotal =
                                    (finalReadingTime?.get("totalSeconds") as? Number)?.toLong()
                                        ?: 0L
                                finalTotal != phoneTotal || (bandTotal > 0 && finalTotal != bandTotal)
                            }

                            SyncMode.BAND_ONLY -> bandReadingTime != null
                            SyncMode.PHONE_ONLY -> false
                        }

                        if (hasProgressChange || hasReadingTimeChange) {
                            changedBooks.add(book.name)
                        }


                        when (currentProgressMode) {
                            SyncMode.AUTO, SyncMode.BAND_ONLY -> {
                                savePhoneReadingProgress(book, finalProgress)
                            }

                            SyncMode.PHONE_ONLY -> {

                                Log.d(
                                    "MainViewModel",
                                    "PHONE_ONLY mode: keeping phone progress unchanged for ${book.name}"
                                )
                            }
                        }

                        when (currentReadingTimeMode) {
                            SyncMode.AUTO, SyncMode.BAND_ONLY -> {
                                savePhoneReadingTime(book.name, finalReadingTime)
                                Log.d("MainViewModel", "Saved reading time for ${book.name}")
                            }

                            SyncMode.PHONE_ONLY -> {

                                Log.d(
                                    "MainViewModel",
                                    "PHONE_ONLY mode: keeping phone reading time unchanged for ${book.name}"
                                )
                            }
                        }


                        val progressJson = when (currentProgressMode) {
                            SyncMode.AUTO, SyncMode.PHONE_ONLY -> {
                                finalProgress?.let { fp ->
                                    try {
                                        val normalized = HashMap<String, Any?>()
                                        normalized.putAll(fp)
                                        val rawOffsetAny = fp["offsetInChapter"]
                                        val rawOffset = when (rawOffsetAny) {
                                            is Number -> rawOffsetAny.toInt()
                                            is String -> rawOffsetAny.toIntOrNull() ?: 0
                                            else -> 0
                                        }
                                        var normalizedOffset = if (rawOffset < 0) 0 else rawOffset
                                        if (normalizedOffset % 2 == 1) normalizedOffset =
                                            Math.max(0, normalizedOffset - 1)
                                        if (fp.containsKey("offsetInChapter")) {
                                            normalized["offsetInChapter"] = normalizedOffset
                                        }
                                        JSONObject(normalized).toString()
                                    } catch (e: Exception) {
                                        Log.e("MainViewModel", "Failed to serialize progress", e)
                                        null
                                    }
                                }
                            }

                            SyncMode.BAND_ONLY -> {
                                Log.d(
                                    "MainViewModel",
                                    "BAND_ONLY mode: keeping band progress unchanged for ${book.name}"
                                )
                                null
                            }
                        }

                        val readingTimeJson = when (currentReadingTimeMode) {
                            SyncMode.AUTO, SyncMode.PHONE_ONLY -> {
                                finalReadingTime?.let {
                                    try {
                                        val json = JSONObject(it).toString()
                                        Log.d(
                                            "MainViewModel",
                                            "Serialized reading time JSON for ${book.name}: ${json.length} chars"
                                        )
                                        json
                                    } catch (e: Exception) {
                                        Log.e(
                                            "MainViewModel",
                                            "Failed to serialize reading time for ${book.name}",
                                            e
                                        )
                                        null
                                    }
                                }
                            }

                            SyncMode.BAND_ONLY -> {

                                Log.d(
                                    "MainViewModel",
                                    "BAND_ONLY mode: keeping band reading time unchanged for ${book.name}"
                                )
                                null
                            }
                        }

                        if (progressJson != null || readingTimeJson != null) {
                            Log.d(
                                "MainViewModel",
                                "Sending reading data to band for ${book.name}: progress=${progressJson != null}, readingTime=${readingTimeJson != null}"
                            )
                            fileConn.setReadingData(book.name, progressJson, readingTimeJson)
                            Log.d(
                                "MainViewModel",
                                "Successfully sent reading data to band for ${book.name}"
                            )
                        } else {
                            Log.d(
                                "MainViewModel",
                                "No reading data to send to band for ${book.name}"
                            )
                        }

                        try {
                            val phoneBookmarks =
                                BookmarkManager.getBookmarksForSync(getApplication(), book.id)
                            val bandBookmarks = try {
                                fileConn.getBookmarks(book.name)
                            } catch (e: Exception) {
                                Log.e(
                                    "MainViewModel",
                                    "Failed to get bookmarks from band for ${book.name}",
                                    e
                                )
                                emptyList()
                            }

                            val currentBookmarkMode = _syncReadingDataState.value.bookmarkSyncMode
                            when (currentBookmarkMode) {
                                SyncMode.PHONE_ONLY -> {
                                    if (phoneBookmarks.isNotEmpty()) {
                                        val bookmarkData = phoneBookmarks.map { bm ->
                                            com.bandbbs.ebook.logic.BookmarkData(
                                                name = bm.name,
                                                chapterIndex = bm.chapterIndex,
                                                chapterName = bm.chapterName,
                                                offsetInChapter = bm.offsetInChapter,
                                                scrollOffset = bm.scrollOffset,
                                                time = bm.time
                                            )
                                        }
                                        fileConn.setBookmarks(book.name, bookmarkData)
                                        Log.d(
                                            "MainViewModel",
                                            "Synced ${bookmarkData.size} bookmarks from phone to band for ${book.name}"
                                        )
                                    }
                                }

                                SyncMode.BAND_ONLY -> {
                                    if (bandBookmarks.isNotEmpty()) {
                                        val bookmarkEntities = bandBookmarks.map { bm ->
                                            BookmarkEntity(
                                                bookId = book.id,
                                                name = bm.name,
                                                chapterIndex = bm.chapterIndex,
                                                chapterName = bm.chapterName,
                                                offsetInChapter = bm.offsetInChapter,
                                                scrollOffset = bm.scrollOffset,
                                                time = bm.time
                                            )
                                        }
                                        BookmarkManager.syncBookmarksFromBand(
                                            getApplication(),
                                            book.id,
                                            bookmarkEntities
                                        )
                                        Log.d(
                                            "MainViewModel",
                                            "Synced ${bookmarkEntities.size} bookmarks from band to phone for ${book.name}"
                                        )
                                    }
                                }

                                SyncMode.AUTO -> {
                                    val mergedBookmarks = mutableListOf<BookmarkEntity>()
                                    val bandBookmarkMap =
                                        bandBookmarks.associateBy { "${it.chapterIndex}_${it.offsetInChapter}" }
                                    phoneBookmarks.associateBy { "${it.chapterIndex}_${it.offsetInChapter}" }

                                    bandBookmarks.forEach { bm ->
                                        mergedBookmarks.add(
                                            BookmarkEntity(
                                                bookId = book.id,
                                                name = bm.name,
                                                chapterIndex = bm.chapterIndex,
                                                chapterName = bm.chapterName,
                                                offsetInChapter = bm.offsetInChapter,
                                                scrollOffset = bm.scrollOffset,
                                                time = bm.time
                                            )
                                        )
                                    }

                                    phoneBookmarks.forEach { bm ->
                                        val key = "${bm.chapterIndex}_${bm.offsetInChapter}"
                                        if (!bandBookmarkMap.containsKey(key)) {
                                            mergedBookmarks.add(bm)
                                        }
                                    }

                                    if (mergedBookmarks.isNotEmpty()) {
                                        val bookmarkData = mergedBookmarks.map { bm ->
                                            com.bandbbs.ebook.logic.BookmarkData(
                                                name = bm.name,
                                                chapterIndex = bm.chapterIndex,
                                                chapterName = bm.chapterName,
                                                offsetInChapter = bm.offsetInChapter,
                                                scrollOffset = bm.scrollOffset,
                                                time = bm.time
                                            )
                                        }
                                        fileConn.setBookmarks(book.name, bookmarkData)
                                        BookmarkManager.syncBookmarksFromBand(
                                            getApplication(),
                                            book.id,
                                            mergedBookmarks
                                        )
                                        Log.d(
                                            "MainViewModel",
                                            "Merged and synced ${mergedBookmarks.size} bookmarks for ${book.name}"
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Failed to sync bookmarks for ${book.name}", e)
                        }

                        syncedCount++
                        withContext(Dispatchers.Main) {
                            _syncReadingDataState.value = _syncReadingDataState.value.copy(
                                syncedBooks = syncedCount
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Failed to sync reading data for ${book.name}", e)
                        failedBooks[book.name] = "同步失败: ${e.message ?: "未知错误"}"
                        withContext(Dispatchers.Main) {
                            val currentFailed =
                                _syncReadingDataState.value.failedBooks.toMutableMap()
                            currentFailed[book.name] = "同步失败: ${e.message ?: "未知错误"}"
                            _syncReadingDataState.value = _syncReadingDataState.value.copy(
                                failedBooks = currentFailed
                            )
                        }
                    }
                }


                loadBooks()

                withContext(Dispatchers.Main) {
                    val statusText = if (failedBooks.isEmpty()) {
                        "同步完成，共同步 $syncedCount 本书"
                    } else {
                        val failedCount = failedBooks.size
                        "同步完成，成功 $syncedCount 本，失败 $failedCount 本"
                    }
                    _syncReadingDataState.value = SyncReadingDataState(
                        isSyncing = false,
                        statusText = statusText,
                        progress = 1f,
                        totalBooks = allBooks.size,
                        syncedBooks = syncedCount,
                        failedBooks = failedBooks
                    )
                    if (changedBooks.isNotEmpty()) {
                        _syncResultState.value = SyncResultState(
                            changedBooks = changedBooks,
                            syncedCount = syncedCount
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {

                    withContext(Dispatchers.Main) {
                        _syncReadingDataState.value = SyncReadingDataState(
                            isSyncing = false,
                            statusText = "同步已取消",
                            progress = 0f
                        )
                    }
                } else {
                    Log.e("MainViewModel", "Failed to sync reading data", e)
                    withContext(Dispatchers.Main) {
                        _syncReadingDataState.value = SyncReadingDataState(
                            isSyncing = false,
                            statusText = "同步失败: ${e.message}",
                            progress = 0f
                        )
                    }
                }
            } finally {
                syncReadingDataJob = null
            }
        }
    }



    fun clearAllReadingTimeData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val readingTimePrefs = getApplication<Application>().getSharedPreferences(
                    "reading_time_prefs",
                    Context.MODE_PRIVATE
                )
                val editor = readingTimePrefs.edit()
                editor.clear()
                editor.apply()
                Log.d("MainViewModel", "Cleared all reading time data")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to clear reading time data", e)
            }
        }
    }

    fun cleanDirtyData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val application = getApplication<Application>()
                val context = application.applicationContext
                val readingTimePrefs =
                    application.getSharedPreferences("reading_time_prefs", Context.MODE_PRIVATE)
                val readerPrefs =
                    application.getSharedPreferences("chapter_reader_prefs", Context.MODE_PRIVATE)

                val books = db.bookDao().getAllBooks()
                books.forEach { book ->
                    val file = File(book.path)
                    if (!file.exists()) {
                        try {
                            ChapterContentManager.deleteBookChapters(context, book.id)
                        } catch (_: Exception) {
                        }
                        try {
                            db.chapterDao().deleteChaptersByBookId(book.id)
                        } catch (_: Exception) {
                        }
                        try {
                            val edit = readerPrefs.edit()
                            edit.remove("last_read_chapter_${book.id}")
                            edit.remove("last_read_timestamp_${book.id}")
                            edit.apply()
                        } catch (_: Exception) {
                        }
                        try {
                            ReadingTimeStorage.clearReadingTime(context, book.name)
                        } catch (_: Exception) {
                        }
                        try {
                            db.bookDao().delete(book)
                        } catch (_: Exception) {
                        }
                    }
                }

                val validBooks = db.bookDao().getAllBooks()
                val validBookIds = validBooks.map { it.id }.toSet()
                val validBookNames = validBooks.map { it.name }.toSet()
                val validChapterIds = mutableSetOf<Int>()
                validBooks.forEach { book ->
                    try {
                        val chapterInfos = db.chapterDao().getChapterInfoForBook(book.id)
                        validChapterIds.addAll(chapterInfos.map { it.id })
                    } catch (_: Exception) {
                    }
                }

                val timeKeys = readingTimePrefs.all.keys.toList()
                timeKeys.forEach { key ->
                    if (key.endsWith("_total_seconds")) {
                        val bookName = key.removeSuffix("_total_seconds")
                        if (!validBookNames.contains(bookName)) {
                            try {
                                ReadingTimeStorage.clearReadingTime(context, bookName)
                            } catch (_: Exception) {
                            }
                        }
                    }
                }

                val readerAllKeys = readerPrefs.all.keys.toList()
                val readerEditor = readerPrefs.edit()
                readerAllKeys.forEach { key ->
                    if (key.startsWith("last_read_chapter_")) {
                        val id = key.removePrefix("last_read_chapter_").toIntOrNull()
                        if (id == null || !validBookIds.contains(id)) {
                            readerEditor.remove(key)
                        }
                    } else if (key.startsWith("last_read_timestamp_")) {
                        val id = key.removePrefix("last_read_timestamp_").toIntOrNull()
                        if (id == null || !validBookIds.contains(id)) {
                            readerEditor.remove(key)
                        }
                    } else if (key.startsWith("reading_position_index_")) {
                        val id = key.removePrefix("reading_position_index_").toIntOrNull()
                        if (id == null || !validChapterIds.contains(id)) {
                            readerEditor.remove(key)
                        }
                    } else if (key.startsWith("reading_position_offset_")) {
                        val id = key.removePrefix("reading_position_offset_").toIntOrNull()
                        if (id == null || !validChapterIds.contains(id)) {
                            readerEditor.remove(key)
                        }
                    }
                }
                readerEditor.apply()
                Log.d("MainViewModel", "Cleaned dirty data")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to clean dirty data", e)
            }
        }
    }

    fun setShowRecentImport(show: Boolean) {
        prefs.edit().putBoolean(SHOW_RECENT_IMPORT_KEY, show).apply()
        _showRecentImport.value = show
    }

    fun setShowRecentUpdate(show: Boolean) {
        prefs.edit().putBoolean(SHOW_RECENT_UPDATE_KEY, show).apply()
        _showRecentUpdate.value = show
    }

    fun setShowSearchBar(show: Boolean) {
        prefs.edit().putBoolean(SHOW_SEARCH_BAR_KEY, show).apply()
        _showSearchBar.value = show
    }

    fun setQuickEditCategory(enabled: Boolean) {
        prefs.edit().putBoolean(QUICK_EDIT_CATEGORY_KEY, enabled).apply()
        _quickEditCategoryEnabled.value = enabled
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(THEME_MODE_KEY, mode.name).apply()
        _themeMode.value = mode
    }

    fun setQuickRenameCategory(enabled: Boolean) {
        prefs.edit().putBoolean(QUICK_RENAME_CATEGORY_KEY, enabled).apply()
        _quickRenameCategoryEnabled.value = enabled
    }

    fun renameCategory(oldName: String, newName: String) {
        categoryHandler.renameCategory(oldName, newName)
    }

    fun dismissUpdateCheck() {
        _updateCheckState.value = UpdateCheckState()
    }

    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private suspend fun getPhoneReadingProgress(book: Book): Map<String, Any>? {
        val lastReadChapterId = readerPrefs.getInt("last_read_chapter_${book.id}", -1)
        if (lastReadChapterId == -1) return null

        db.chapterDao().getChapterById(lastReadChapterId) ?: return null
        val allChapters = db.chapterDao().getChapterInfoForBook(book.id)
        val chapterIndex = allChapters.indexOfFirst { it.id == lastReadChapterId }
        if (chapterIndex == -1) return null

        readerPrefs.getInt("reading_position_index_$lastReadChapterId", 0)
        val offset = readerPrefs.getInt("reading_position_offset_$lastReadChapterId", 0)
        val lastReadTimestamp = readerPrefs.getLong("last_read_timestamp_${book.id}", 0L)

        return mapOf(
            "chapterIndex" to chapterIndex,
            "offsetInChapter" to offset,
            "scrollOffset" to 0,
            "lastReadTimestamp" to (if (lastReadTimestamp > 0L) lastReadTimestamp else System.currentTimeMillis())
        )
    }


    private fun mergeProgress(
        phoneProgress: Map<String, Any>?,
        bandProgress: Map<String, Any>?
    ): Map<String, Any>? {
        if (phoneProgress == null && bandProgress == null) return null
        if (phoneProgress == null) return bandProgress
        if (bandProgress == null) return phoneProgress

        val phoneTimestamp = (phoneProgress["lastReadTimestamp"] as? Number)?.toLong() ?: 0L
        val bandTimestamp = (bandProgress["lastReadTimestamp"] as? Number)?.toLong() ?: 0L

        return if (phoneTimestamp >= bandTimestamp) phoneProgress else bandProgress
    }


    private suspend fun savePhoneReadingProgress(book: Book, progress: Map<String, Any>?) {
        if (progress == null) return

        val chapterIndex = (progress["chapterIndex"] as? Number)?.toInt()
        if (chapterIndex != null && chapterIndex >= 0) {
            val allChapters = db.chapterDao().getChapterInfoForBook(book.id)
            if (chapterIndex < allChapters.size && allChapters.isNotEmpty()) {
                val chapterId = allChapters[chapterIndex].id
                val offset = (progress["offsetInChapter"] as? Number)?.toInt() ?: 0
                val timestamp = (progress["lastReadTimestamp"] as? Number)?.toLong() ?: 0L



                if (timestamp > 0L || chapterIndex > 0 || offset > 0) {
                    readerPrefs.edit()
                        .putInt("last_read_chapter_${book.id}", chapterId)
                        .putInt("reading_position_offset_$chapterId", offset)
                        .putLong(
                            "last_read_timestamp_${book.id}",
                            if (timestamp > 0L) timestamp else System.currentTimeMillis()
                        )
                        .apply()
                    Log.d(
                        "MainViewModel",
                        "Saved progress for ${book.name}: chapterIndex=$chapterIndex, chapterId=$chapterId, offset=$offset, timestamp=$timestamp"
                    )
                } else {
                    Log.w(
                        "MainViewModel",
                        "Skipping save progress for ${book.name}: invalid data (chapterIndex=$chapterIndex, offset=$offset, timestamp=$timestamp)"
                    )
                }
            } else {
                Log.w(
                    "MainViewModel",
                    "Invalid chapterIndex $chapterIndex for ${book.name} (total chapters: ${allChapters.size})"
                )
            }
        } else {
            Log.w("MainViewModel", "No valid chapterIndex in progress for ${book.name}")
        }
    }

    private fun getPhoneReadingTime(bookName: String): Map<String, Any>? {
        val readingTimePrefs = getApplication<Application>().getSharedPreferences(
            "reading_time_prefs",
            Context.MODE_PRIVATE
        )
        val totalSeconds = readingTimePrefs.getLong("${bookName}_total_seconds", 0L)
        Log.d("MainViewModel", "getPhoneReadingTime($bookName): totalSeconds=$totalSeconds")
        if (totalSeconds == 0L) {
            Log.d("MainViewModel", "No reading time data found for $bookName")
            return null
        }

        val sessionsJson = readingTimePrefs.getString("${bookName}_sessions", null)
        val sessions = if (sessionsJson != null) {
            try {
                org.json.JSONArray(sessionsJson)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to parse sessions JSON for $bookName", e)
                null
            }
        } else null

        val sessionList = sessions?.let {
            (0 until it.length()).map { i ->
                try {
                    it.getJSONObject(i)
                } catch (e: Exception) {
                    null
                }
            }.filterNotNull()
        } ?: emptyList<Any>()

        val lastReadDate = readingTimePrefs.getString("${bookName}_last_read_date", null) ?: ""
        val firstReadDate = readingTimePrefs.getString("${bookName}_first_read_date", null) ?: ""

        Log.d(
            "MainViewModel",
            "getPhoneReadingTime($bookName): sessions=${sessionList.size}, firstReadDate=$firstReadDate, lastReadDate=$lastReadDate"
        )

        return mapOf(
            "totalSeconds" to totalSeconds,
            "sessions" to sessionList,
            "lastReadDate" to lastReadDate,
            "firstReadDate" to firstReadDate
        )
    }

    private fun savePhoneReadingTime(bookName: String, readingTime: Map<String, Any>?) {
        if (readingTime == null) {
            Log.d("MainViewModel", "savePhoneReadingTime($bookName): skipping, readingTime is null")
            return
        }

        val readingTimePrefs = getApplication<Application>().getSharedPreferences(
            "reading_time_prefs",
            Context.MODE_PRIVATE
        )
        val totalSeconds = (readingTime["totalSeconds"] as? Number)?.toLong() ?: 0L
        val lastReadDate = readingTime["lastReadDate"] as? String ?: ""
        val firstReadDate = readingTime["firstReadDate"] as? String ?: ""

        Log.d(
            "MainViewModel",
            "savePhoneReadingTime($bookName): totalSeconds=$totalSeconds, firstReadDate=$firstReadDate, lastReadDate=$lastReadDate"
        )

        val editor = readingTimePrefs.edit()
        editor.putLong("${bookName}_total_seconds", totalSeconds)
        if (lastReadDate.isNotEmpty()) {
            editor.putString("${bookName}_last_read_date", lastReadDate)
        }
        if (firstReadDate.isNotEmpty()) {
            editor.putString("${bookName}_first_read_date", firstReadDate)
        }

        val sessions = readingTime["sessions"]
        if (sessions is List<*>) {
            try {
                val sessionsArray = org.json.JSONArray()
                sessions.forEach { session ->
                    if (session is Map<*, *>) {
                        val sessionObj = JSONObject()
                        session.forEach { (key, value) ->
                            when (value) {
                                is Number -> sessionObj.put(key.toString(), value)
                                is String -> sessionObj.put(key.toString(), value)
                                is Boolean -> sessionObj.put(key.toString(), value)
                                else -> sessionObj.put(key.toString(), value.toString())
                            }
                        }
                        sessionsArray.put(sessionObj)
                    } else if (session is JSONObject) {
                        sessionsArray.put(session)
                    }
                }
                editor.putString("${bookName}_sessions", sessionsArray.toString())
                Log.d("MainViewModel", "Saved ${sessionsArray.length()} sessions for $bookName")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to save sessions for $bookName", e)
            }
        } else {
            Log.d("MainViewModel", "No sessions to save for $bookName")
        }

        editor.apply()
        Log.d("MainViewModel", "Successfully saved reading time for $bookName")
    }

    private fun mergeReadingTime(
        phoneReadingTime: Map<String, Any>?,
        bandReadingTime: Map<String, Any>?
    ): Map<String, Any>? {
        if (phoneReadingTime == null && bandReadingTime == null) return null
        if (phoneReadingTime == null) return bandReadingTime
        if (bandReadingTime == null) return phoneReadingTime

        val phoneTotalSeconds = (phoneReadingTime["totalSeconds"] as? Number)?.toLong() ?: 0L
        val bandTotalSeconds = (bandReadingTime["totalSeconds"] as? Number)?.toLong() ?: 0L

        return if (phoneTotalSeconds >= bandTotalSeconds) phoneReadingTime else bandReadingTime
    }


    private fun loadBooks() {
        viewModelScope.launch(Dispatchers.IO) {
            val bookEntities = db.bookDao().getAllBooks()

            val bookUiModels = bookEntities.map { entity ->
                val chapterCount = db.chapterDao().getChapterCountForBook(entity.id)
                val wordCount = db.chapterDao().getTotalWordCountForBook(entity.id) ?: 0

                val lastReadChapterId = readerPrefs.getInt("last_read_chapter_${entity.id}", -1)
                var lastReadInfo: String? = null
                var chapterIndex: Int? = null
                var chapterProgressPercent: Float = 0f

                if (lastReadChapterId != -1) {
                    val chapter = db.chapterDao().getChapterById(lastReadChapterId)
                    if (chapter != null) {
                        val allChapters = db.chapterDao().getChapterInfoForBook(entity.id)
                        chapterIndex = allChapters.indexOfFirst { it.id == lastReadChapterId }
                        if (chapterIndex != -1 && allChapters.isNotEmpty()) {
                            chapterProgressPercent =
                                (chapterIndex + 1).toFloat() / allChapters.size * 100f
                        }
                        lastReadInfo = "读至：${chapter.name}"
                    }
                }
                if (lastReadInfo == null && chapterCount > 0) {
                    lastReadInfo = "未读"
                }

                val lastReadTimestamp = readerPrefs.getLong("last_read_timestamp_${entity.id}", 0L)

                Book(
                    id = entity.id,
                    name = entity.name,
                    path = entity.path,
                    size = entity.size,
                    format = entity.format,
                    chapterCount = chapterCount,
                    wordCount = wordCount,
                    syncedChapterCount = 0,
                    coverImagePath = entity.coverImagePath,
                    localCategory = entity.localCategory,
                    lastReadInfo = lastReadInfo,
                    lastReadTimestamp = lastReadTimestamp,
                    chapterIndex = chapterIndex,
                    chapterProgressPercent = chapterProgressPercent
                )
            }

            val recentUpdatedBook = bookUiModels.maxByOrNull { book ->
                try {
                    File(book.path).lastModified()
                } catch (e: Exception) {
                    0L
                }
            }

            withContext(Dispatchers.Main) {
                _books.value = bookUiModels.sortedByDescending { it.name }

                _recentBook.value = bookUiModels.maxByOrNull { it.id }
                _recentUpdatedBook.value = recentUpdatedBook
            }
        }
    }

    fun backupData(uri: Uri) {
        viewModelScope.launch {
            val result = DataBackupManager.backupData(getApplication(), uri, db)
            _backupRestoreState.value = if (result.isSuccess) {
                BackupRestoreResult(success = true, message = "备份成功")
            } else {
                BackupRestoreResult(
                    success = false,
                    message = "备份失败: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun restoreData(uri: Uri) {
        viewModelScope.launch {
            val result = DataBackupManager.restoreData(getApplication(), uri, db)
            if (result.isSuccess) {
                loadBooks()
                _backupRestoreState.value =
                    BackupRestoreResult(success = true, message = "恢复成功")
            } else {
                _backupRestoreState.value = BackupRestoreResult(
                    success = false,
                    message = "恢复失败: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun clearBackupRestoreState() {
        _backupRestoreState.value = null
    }


}
