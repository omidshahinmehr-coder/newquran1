package com.lbo.quran.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.snapshotFlow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lbo.quran.data.ReadingItem
import com.lbo.quran.ui.theme.quranFontByKey
import com.lbo.quran.ui.theme.translationFontByKey
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: QuranViewModel,
    onOpenSurahPicker: () -> Unit,
    onOpenJuzPicker: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenTafsirBrowse: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenTafsir: (aId: String, surahName: String, ayahNumber: Int) -> Unit
) {
    val state by viewModel.fullQuran.collectAsState()
    val scrollTarget by viewModel.scrollTarget.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var showHint by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.loadFullQuran()
    }

    // بازگشت از صفحه تفسیر: اسکرول به همان آیه‌ای که تفسیرش باز شده بود
    // یا (فقط بار اول در طول عمر برنامه) اسکرول به آخرین محل مطالعه ذخیره‌شده
    LaunchedEffect(state.items.size) {
        if (state.items.isNotEmpty()) {
            val returnAyah = viewModel.consumePendingReturnAyah()
            val target = returnAyah ?: viewModel.consumeInitialScrollAyah()
            target?.let { aId ->
                viewModel.itemIndexForAyah(aId)?.let { index ->
                    listState.scrollToItem(index)
                }
            }
        }
    }

    // ذخیره خودکار آخرین آیه‌ای که کاربر در حال مشاهده آن است (با تأخیر کوتاه، برای جلوگیری از نوشتن مکرر)
    LaunchedEffect(state.items) {
        if (state.items.isEmpty()) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .debounce(1000)
            .collect { index ->
                val visibleAyah = state.items.drop(index)
                    .firstOrNull { it is ReadingItem.Ayah } as? ReadingItem.Ayah
                visibleAyah?.let { viewModel.saveLastReadPosition(it.ayah.aId) }
            }
    }

    // انتخاب سوره/جزء از منو، حتی وقتی صفحه اصلی از قبل باز است
    LaunchedEffect(scrollTarget) {
        scrollTarget?.let { index ->
            listState.scrollToItem(index)
            viewModel.consumeScrollTarget()
        }
    }

    // نام سوره‌ای که در حال حاضر بالای صفحه دیده می‌شود؛ با اسکرول به‌روزرسانی می‌شود
    val currentSurahName by remember {
        derivedStateOf {
            val items = state.items
            if (items.isEmpty()) return@derivedStateOf ""
            var idx = listState.firstVisibleItemIndex.coerceIn(0, items.size - 1)
            var name = ""
            while (idx >= 0) {
                when (val current = items[idx]) {
                    is ReadingItem.SurahHeader -> { name = current.surahNameFa; break }
                    is ReadingItem.Ayah -> { name = current.surahNameFa; break }
                    is ReadingItem.Bismillah -> { idx-- }
                }
            }
            name
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text(
                    "قرآن کریم",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("فهرست سوره‌ها") },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onOpenSurahPicker() },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("فهرست اجزاء") },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onOpenJuzPicker() },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("نشانک‌های من") },
                    icon = { Icon(Icons.Default.Bookmarks, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onOpenBookmarks() },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "زبان ترجمه",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    FilterChip(
                        selected = settings.translationLanguage == "fa",
                        onClick = { viewModel.updateSettings(settings.copy(translationLanguage = "fa")) },
                        label = { Text("فارسی") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    FilterChip(
                        selected = settings.translationLanguage == "en",
                        onClick = { viewModel.updateSettings(settings.copy(translationLanguage = "en")) },
                        label = { Text("English") }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                    label = { Text("تفسیر البرهان") },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onOpenTafsirBrowse() },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("تنظیمات نمایش") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onOpenSettings() },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("درباره برنامه") },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onOpenAbout() },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text("قرآن کریم") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "منو")
                            }
                        },
                        actions = {
                            Text(
                                "ترجمه",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Switch(
                                checked = state.showTranslation,
                                onCheckedChange = { viewModel.toggleFullQuranTranslationVisible() }
                            )
                            IconButton(onClick = onOpenSearch) {
                                Icon(Icons.Default.Search, contentDescription = "جستجو")
                            }
                        }
                    )
                    if (currentSurahName.isNotBlank()) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(
                                currentSurahName,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            if (state.loading) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
            ) {
                if (showHint) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "برای مشاهده تفسیر البرهان یا اشتراک‌گذاری، از آیکون‌های زیر هر آیه استفاده کنید. برای رفتن به سوره یا جزء دیگر از منو استفاده کنید.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { showHint = false }) {
                                    Text("×", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }

                items(state.items) { item ->
                    when (item) {
                        is ReadingItem.SurahHeader -> {
                            Text(
                                item.surahNameFa,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp)
                            )
                        }
                        is ReadingItem.Bismillah -> {
                            Text(
                                "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                fontFamily = quranFontByKey(settings.quranFontKey),
                                fontSize = settings.quranFontSize.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            )
                        }
                        is ReadingItem.Ayah -> {
                            val ayah = item.ayah
                            val surahNameFa = item.surahNameFa
                            val hasTafsir = ayah.aId in state.ayahIdsWithTafsir
                            val context = LocalContext.current
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(settings.quranBackgroundColor))
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    SelectionContainer {
                                        Column {
                                            Text(
                                                "${ayah.text}  (${ayah.ayahNumber})",
                                                fontFamily = quranFontByKey(settings.quranFontKey),
                                                fontSize = settings.quranFontSize.sp,
                                                lineHeight = (settings.quranFontSize * 1.9).sp,
                                                color = Color(settings.quranTextColor),
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            if (state.showTranslation) {
                                                state.translations[ayah.aId]?.let { tr ->
                                                    Spacer(Modifier.height(8.dp))
                                                    val isEnglish = settings.translationLanguage == "en"
                                                    Text(
                                                        tr.text,
                                                        fontFamily = if (isEnglish) null else translationFontByKey(settings.translationFontKey),
                                                        fontSize = settings.translationFontSize.sp,
                                                        lineHeight = (settings.translationFontSize * 1.6).sp,
                                                        color = Color(settings.quranTextColor),
                                                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isBookmarked = ayah.aId in state.bookmarkedAyahIds
                                        IconButton(
                                            onClick = { viewModel.toggleBookmark(ayah.aId) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = if (isBookmarked) "حذف نشانک" else "نشانک‌گذاری",
                                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        if (hasTafsir) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.rememberReturnAyah(ayah.aId)
                                                    onOpenTafsir(ayah.aId, surahNameFa, ayah.ayahNumber)
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.MenuBook,
                                                    contentDescription = "نمایش تفسیر",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                val shareBody = buildString {
                                                    append(ayah.text)
                                                    if (state.showTranslation) {
                                                        state.translations[ayah.aId]?.let {
                                                            append("\n\n")
                                                            append(it.text)
                                                        }
                                                    }
                                                    append("\n\n")
                                                    append("$surahNameFa — ${ayah.ayahNumber}")
                                                }
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, shareBody)
                                                }
                                                context.startActivity(Intent.createChooser(intent, null))
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Share,
                                                contentDescription = "اشتراک‌گذاری آیه",
                                                modifier = Modifier.size(20.dp)
                                            )
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
