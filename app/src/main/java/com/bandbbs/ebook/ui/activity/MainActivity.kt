package com.bandbbs.ebook.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.ui.NavDisplay
import com.bandbbs.ebook.ui.screens.ChapterListScreen
import com.bandbbs.ebook.ui.screens.MainScreen
import com.bandbbs.ebook.ui.screens.ReaderScreen
import com.bandbbs.ebook.ui.screens.SettingsScreen
import com.bandbbs.ebook.ui.screens.StatisticsScreen
import com.bandbbs.ebook.ui.theme.EbookTheme
import com.bandbbs.ebook.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.FabPosition
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.icon.extended.VerticalSplit
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface Screen : NavKey {
    data object HomePager : Screen // 将原来的 Home、Statistics、Settings 整合为此 Pager 页面
    data object ChapterList : Screen
    data object Reader : Screen
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) {
                if (uris.size == 1) viewModel.startImport(uris[0]) else viewModel.startImportBatch(
                    uris
                )
            }
        }

    private val coverPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.importCoverForBook(it) }
        }

    private val createDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let { viewModel.backupData(it) }
        }

    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.restoreData(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val firstLaunchTutorial = !prefs.getBoolean("tutorial_shown", false)
        val markTutorialShown = { prefs.edit().putBoolean("tutorial_shown", true).apply() }

        observeViewModelStates()

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                MainViewModel.ThemeMode.LIGHT -> false
                MainViewModel.ThemeMode.DARK -> true
                MainViewModel.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            EbookTheme(darkTheme = darkTheme) {
                val chapterToPreview by viewModel.chapterToPreview.collectAsState()
                val selectedBookForChapters by viewModel.selectedBookForChapters.collectAsState()
                val chaptersForSelectedBook by viewModel.chaptersForSelectedBook.collectAsState()
                val globalLoadingState by viewModel.globalLoadingState.collectAsState()

                val backStack = remember { mutableStateListOf<NavKey>(Screen.HomePager) }
                val currentScreen = backStack.lastOrNull() ?: Screen.HomePager
                val statisticsScrollState = rememberScrollState()

                val pagerState = rememberPagerState(pageCount = { 3 })

                val navigateTo = { screen: Screen ->
                    if (backStack.lastOrNull() != screen) {
                        backStack.add(screen)
                    }
                }
                val navigateBack = {
                    if (backStack.size > 1) {
                        backStack.removeAt(backStack.size - 1)
                    }
                }
                val navigateToHome = {
                    backStack.clear()
                    backStack.add(Screen.HomePager)
                }

                LaunchedEffect(selectedBookForChapters) {
                    if (selectedBookForChapters != null) navigateTo(Screen.ChapterList)
                }
                LaunchedEffect(chapterToPreview) {
                    if (chapterToPreview != null) navigateTo(Screen.Reader)
                }

                val showBottomBar = currentScreen is Screen.HomePager

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = pagerState.currentPage == 0,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                                    icon = MiuixIcons.VerticalSplit,
                                    label = "主页"
                                )
                                NavigationBarItem(
                                    selected = pagerState.currentPage == 1,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                                    icon = MiuixIcons.Sort,
                                    label = "统计"
                                )
                                NavigationBarItem(
                                    selected = pagerState.currentPage == 2,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                                    icon = MiuixIcons.Settings,
                                    label = "设置"
                                )
                            }
                        }
                    },
                    floatingActionButton = {
                        if (currentScreen is Screen.HomePager && pagerState.currentPage == 0) {
                            FloatingActionButton(
                                modifier = Modifier
                                    .pressable(
                                        interactionSource = null,
                                        indication = SinkFeedback()
                                    ),
                                onClick = {
                                    filePickerLauncher.launch(
                                        arrayOf(
                                            "text/plain",
                                            "application/epub+zip",
                                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                            "application/pdf",
                                            "application/x-pdf",
                                            "application/x-mobipocket-ebook",
                                            "application/vnd.amazon.ebook",
                                            "application/octet-stream"
                                        )
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Add,
                                    tint = MiuixTheme.colorScheme.onPrimary,
                                    contentDescription = "导入书籍"
                                )
                            }
                        }
                    },
                    floatingActionButtonPosition = FabPosition.End
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if (showBottomBar) paddingValues.calculateBottomPadding() else 0.dp)
                    ) {

                        val entryProvider = remember(backStack) {
                            entryProvider<NavKey> {
                                entry<Screen.HomePager> {
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxSize()
                                    ) { page ->
                                        when (page) {
                                            0 -> MainScreen(
                                                viewModel = viewModel,
                                                onImportCoverClick = {
                                                    coverPickerLauncher.launch(
                                                        arrayOf("image/*")
                                                    )
                                                }
                                            )

                                            1 -> StatisticsScreen(
                                                onBackClick = {
                                                    scope.launch {
                                                        pagerState.animateScrollToPage(
                                                            0
                                                        )
                                                    }
                                                },
                                                onBookStatClick = { bookName ->
                                                    BookStatisticsActivity.start(
                                                        this@MainActivity,
                                                        bookName
                                                    )
                                                },
                                                scrollState = statisticsScrollState
                                            )

                                            2 -> SettingsScreen(
                                                viewModel = viewModel,
                                                onBackClick = {
                                                    scope.launch {
                                                        pagerState.animateScrollToPage(
                                                            0
                                                        )
                                                    }
                                                },
                                                onBackupClick = {
                                                    val dateStr = SimpleDateFormat(
                                                        "yyyyMMdd_HHmmss",
                                                        Locale.getDefault()
                                                    ).format(Date())
                                                    createDocumentLauncher.launch("SineEbook_Backup_$dateStr.json")
                                                },
                                                onRestoreClick = {
                                                    openDocumentLauncher.launch(
                                                        arrayOf("application/json")
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                                entry<Screen.ChapterList> {
                                    selectedBookForChapters?.let { book ->
                                        ChapterListScreen(
                                            book = book,
                                            chapters = chaptersForSelectedBook,
                                            readOnly = chapterToPreview != null,
                                            onBackClick = {
                                                viewModel.closeChapterList()
                                                navigateBack()
                                            },
                                            onPreviewChapter = { chapterId ->
                                                scope.launch {
                                                    viewModel.showChapterPreview(chapterId)
                                                }
                                            },
                                            onEditContent = { chapterId ->
                                                viewModel.openChapterEditor(
                                                    chapterId
                                                )
                                            },
                                            onSaveChapterContent = { chapterId, title, content ->
                                                viewModel.saveChapterContent(
                                                    chapterId,
                                                    title,
                                                    content
                                                )
                                            },
                                            onRenameChapter = { chapterId, title ->
                                                viewModel.renameChapter(
                                                    chapterId,
                                                    title
                                                )
                                            },
                                            onAddChapter = { index, title, content ->
                                                viewModel.addChapter(
                                                    index,
                                                    title,
                                                    content
                                                )
                                            },
                                            onMergeChapters = { ids, title, insertBlank ->
                                                viewModel.mergeChapters(
                                                    ids,
                                                    title,
                                                    insertBlank
                                                )
                                            },
                                            onBatchRename = { ids, prefix, suffix, startNumber, padding ->
                                                viewModel.batchRenameChapters(
                                                    ids,
                                                    prefix,
                                                    suffix,
                                                    startNumber,
                                                    padding
                                                )
                                            },
                                            loadChapterContent = { chapterId ->
                                                viewModel.loadChapterContent(
                                                    chapterId
                                                )
                                            }
                                        )
                                    }
                                }
                                entry<Screen.Reader> {
                                    ReaderScreen(
                                        viewModel = viewModel,
                                        onClose = {
                                            viewModel.closeChapterPreview()
                                            navigateBack()
                                        },
                                        onChapterChange = { chapterId ->
                                            viewModel.showChapterPreview(
                                                chapterId
                                            )
                                        },
                                        onTableOfContents = {
                                            viewModel.chapterToPreview.value?.let {
                                                val bookId =
                                                    viewModel.chaptersForPreview.value.firstOrNull()?.bookId
                                                val currentBook =
                                                    viewModel.books.value.find { it.id == bookId }
                                                currentBook?.let { viewModel.showChapterList(it) }
                                            }
                                        },
                                        loadChapterContent = viewModel::loadChapterContent
                                    )
                                }
                            }
                        }

                        val entries = rememberDecoratedNavEntries(
                            backStack = backStack,
                            entryProvider = entryProvider
                        )

                        NavDisplay(
                            entries = entries,
                            onBack = {
                                if (backStack.size > 1) {
                                    navigateBack()
                                } else {
                                    finish()
                                }
                            }
                        )

                        val showTutorialState = remember { mutableStateOf(firstLaunchTutorial) }
                        if (showTutorialState.value) {
                            SuperDialog(
                                title = "首次使用提示",
                                summary = "欢迎使用弦电子书！这是一个功能强大的离线阅读器，支持多种格式的电子书。",
                                show = showTutorialState,
                                onDismissRequest = {
                                    markTutorialShown()
                                    showTutorialState.value = false
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextButton(
                                        text = "关闭",
                                        onClick = {
                                            markTutorialShown()
                                            showTutorialState.value = false
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(20.dp))
                                    TextButton(
                                        text = "我知道了",
                                        onClick = {
                                            markTutorialShown()
                                            showTutorialState.value = false
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.textButtonColorsPrimary()
                                    )
                                }
                            }
                        }

                        val showLoadingState = remember { mutableStateOf(false) }
                        LaunchedEffect(globalLoadingState.isLoading) {
                            showLoadingState.value = globalLoadingState.isLoading
                        }
                        if (showLoadingState.value) {
                            SuperDialog(
                                title = "处理中",
                                summary = globalLoadingState.message,
                                show = showLoadingState,
                                onDismissRequest = {}
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeViewModelStates() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.backupRestoreState.collect { state ->
                    state?.let {
                        if (it.message.isNotEmpty()) {
                            Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_SHORT).show()
                            viewModel.clearBackupRestoreState()
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { viewModel.startImport(it) }
    }
}

