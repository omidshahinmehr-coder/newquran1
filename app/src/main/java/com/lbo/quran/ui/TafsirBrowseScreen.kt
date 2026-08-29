package com.lbo.quran.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lbo.quran.ui.theme.translationFontByKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafsirBrowseScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.tafsirBrowse.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val surahListState by viewModel.surahList.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadTafsirBrowse(null) // پیش‌فرض: کل کتاب
        if (surahListState.surahs.isEmpty()) viewModel.loadSurahList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تفسیر البرهان") },
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

            SurahFilterDropdown(
                surahs = surahListState.surahs,
                selectedSurah = state.surahFilter,
                onSelect = { surahNumber -> viewModel.loadTafsirBrowse(surahNumber) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.updateTafsirBrowseQuery(it) },
                label = { Text("جستجو در متن تفسیر") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))

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
                        if (state.searchQuery.isBlank()) "تفسیری یافت نشد."
                        else "عبارتی یافت نشد."
                    )
                }
                return@Column
            }

            SelectionContainer {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(results) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(settings.tafsirBackgroundColor))
                        ) {
                            Text(
                                entry.text,
                                fontFamily = translationFontByKey(settings.translationFontKey),
                                fontSize = settings.translationFontSize.sp,
                                lineHeight = (settings.translationFontSize * 1.6).sp,
                                color = Color(settings.tafsirTextColor),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurahFilterDropdown(
    surahs: List<com.lbo.quran.data.SurahInfo>,
    selectedSurah: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = surahs.firstOrNull { it.surahNumber == selectedSurah }?.nameFa ?: "کل کتاب"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("فیلتر سوره") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("کل کتاب") },
                onClick = { onSelect(null); expanded = false }
            )
            surahs.forEach { surah ->
                DropdownMenuItem(
                    text = { Text("${surah.surahNumber}. ${surah.nameFa}") },
                    onClick = { onSelect(surah.surahNumber); expanded = false }
                )
            }
        }
    }
}
