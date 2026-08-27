package com.lbo.quran.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lbo.quran.ui.theme.translationFontByKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafsirBrowseScreen(
    viewModel: QuranViewModel,
    surahNumber: Int,
    onBack: () -> Unit
) {
    val state by viewModel.tafsirBrowse.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(surahNumber) {
        viewModel.loadTafsirBrowse(surahNumber)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.surahName.ifBlank { "تفسیر" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = if (state.language == "fa") 1 else 0) {
                Tab(
                    selected = state.language == "ar",
                    onClick = { viewModel.setTafsirBrowseLanguage("ar") },
                    text = { Text("عربی") }
                )
                Tab(
                    selected = state.language == "fa",
                    onClick = { viewModel.setTafsirBrowseLanguage("fa") },
                    text = { Text("فارسی") }
                )
            }

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.updateTafsirBrowseQuery(it) },
                label = { Text("جستجو در متن تفسیر این سوره") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            val results = state.filteredEntries
            if (results.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (state.searchQuery.isBlank()) "تفسیری برای این سوره ثبت نشده است."
                        else "عبارتی یافت نشد."
                    )
                }
                return@Column
            }

            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(results) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(settings.tafsirBackgroundColor))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val sourceLabel = if (entry.language == "fa") "ترجمه تفسیر البرهان" else "تفسیر البرهان"
                                Text(
                                    if (entry.startId == entry.endId)
                                        "آیه مرتبط: ${entry.startId}"
                                    else "آیات مرتبط: ${entry.startId} تا ${entry.endId}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(settings.tafsirTextColor)
                                )
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "${entry.text}\n\n$sourceLabel — ${state.surahName}"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(intent, null))
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "اشتراک‌گذاری",
                                        tint = Color(settings.tafsirTextColor),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            SelectionContainer {
                                Text(
                                    entry.text,
                                    fontFamily = translationFontByKey(settings.translationFontKey),
                                    fontSize = settings.translationFontSize.sp,
                                    lineHeight = (settings.translationFontSize * 1.6).sp,
                                    color = Color(settings.tafsirTextColor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
