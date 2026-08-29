package com.lbo.quran.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lbo.quran.data.AppSettings
import com.lbo.quran.data.AyahEntity
import com.lbo.quran.data.JuzInfo
import com.lbo.quran.data.QuranRepository
import com.lbo.quran.data.ReadingItem
import com.lbo.quran.data.ReadingProgressRepository
import com.lbo.quran.data.SearchResult
import com.lbo.quran.data.SettingsRepository
import com.lbo.quran.data.SurahInfo
import com.lbo.quran.data.TAFSIR_LANG_AR
import com.lbo.quran.data.TR_LANG_EN
import com.lbo.quran.data.TR_LANG_FA
import com.lbo.quran.data.TafsirEntity
import com.lbo.quran.data.TranslationEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BookmarksUiState(
    val bookmarks: List<AyahEntity> = emptyList(),
    val surahNames: Map<Int, String> = emptyMap(),
    val loading: Boolean = true
)

data class SurahListUiState(
    val surahs: List<SurahInfo> = emptyList(),
    val loading: Boolean = true
)

data class FullQuranUiState(
    val items: List<ReadingItem> = emptyList(),
    val translations: Map<String, TranslationEntity> = emptyMap(),
    val ayahIdsWithTafsir: Set<String> = emptySet(),
    val bookmarkedAyahIds: Set<String> = emptySet(),
    val ayahItemIndex: Map<String, Int> = emptyMap(), // a_id -> index in items
    val surahItemIndex: Map<Int, Int> = emptyMap(), // surahNumber -> index of its header
    val juzAyahIndex: Map<Int, Int> = emptyMap(), // juzNumber -> index of its first ayah
    val showTranslation: Boolean = true,
    val loading: Boolean = true
)

data class JuzListUiState(
    val juzList: List<JuzInfo> = emptyList(),
    val loading: Boolean = true
)

data class TafsirUiState(
    val surahName: String = "",
    val ayahNumber: Int = 0,
    val entriesAr: List<TafsirEntity> = emptyList(),
    val entriesFa: List<TafsirEntity> = emptyList(),
    val language: String = "ar",
    val loading: Boolean = true
) {
    val entries: List<TafsirEntity>
        get() = if (language == "fa") entriesFa else entriesAr
}

data class TafsirBrowseUiState(
    val surahFilter: Int? = null, // null یعنی کل کتاب
    val entriesAr: List<TafsirEntity> = emptyList(),
    val entriesFa: List<TafsirEntity> = emptyList(),
    val language: String = "ar",
    val searchQuery: String = "",
    val loading: Boolean = true
) {
    val filteredEntries: List<TafsirEntity>
        get() {
            val entries = if (language == "fa") entriesFa else entriesAr
            return if (searchQuery.isBlank()) entries
            else entries.filter { it.text.contains(searchQuery, ignoreCase = true) }
        }
}

data class SearchUiState(
    val query: String = "",
    val includeQuran: Boolean = true,
    val includeTranslation: Boolean = true,
    val includeTafsir: Boolean = true,
    val results: List<SearchResult> = emptyList(),
    val loading: Boolean = false
)

class QuranViewModel(
    private val repo: QuranRepository,
    private val settingsRepo: SettingsRepository,
    private val progressRepo: ReadingProgressRepository
) : ViewModel() {

    private val _settings = MutableStateFlow(settingsRepo.load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _bookmarksScreen = MutableStateFlow(BookmarksUiState())
    val bookmarksScreen: StateFlow<BookmarksUiState> = _bookmarksScreen.asStateFlow()

    private var appliedInitialScroll = false

    private val _tafsirBrowse = MutableStateFlow(TafsirBrowseUiState())
    val tafsirBrowse: StateFlow<TafsirBrowseUiState> = _tafsirBrowse.asStateFlow()

    private val _fullQuran = MutableStateFlow(FullQuranUiState())
    val fullQuran: StateFlow<FullQuranUiState> = _fullQuran.asStateFlow()

    private val _scrollTarget = MutableStateFlow<Int?>(null)
    val scrollTarget: StateFlow<Int?> = _scrollTarget.asStateFlow()

    private var pendingReturnAyahId: String? = null

    private val _surahList = MutableStateFlow(SurahListUiState())
    val surahList: StateFlow<SurahListUiState> = _surahList.asStateFlow()

    private val _juzList = MutableStateFlow(JuzListUiState())
    val juzList: StateFlow<JuzListUiState> = _juzList.asStateFlow()

    private val _tafsir = MutableStateFlow(TafsirUiState())
    val tafsir: StateFlow<TafsirUiState> = _tafsir.asStateFlow()

    private val _search = MutableStateFlow(SearchUiState())
    val search: StateFlow<SearchUiState> = _search.asStateFlow()

    fun updateSettings(newSettings: AppSettings) {
        val langChanged = newSettings.translationLanguage != _settings.value.translationLanguage
        _settings.value = newSettings
        settingsRepo.save(newSettings)
        if (langChanged) refreshTranslations()
    }

    fun loadTafsirBrowse(surahNumber: Int?) = viewModelScope.launch {
        val keepLang = _tafsirBrowse.value.language
        _tafsirBrowse.value = TafsirBrowseUiState(surahFilter = surahNumber, language = keepLang, loading = true)
        val entriesAr = if (surahNumber == null) repo.getAllTafsir("ar") else repo.getTafsirForSurah(surahNumber, "ar")
        val entriesFa = if (surahNumber == null) repo.getAllTafsir("fa") else repo.getTafsirForSurah(surahNumber, "fa")
        _tafsirBrowse.value = TafsirBrowseUiState(
            surahFilter = surahNumber,
            entriesAr = entriesAr,
            entriesFa = entriesFa,
            language = keepLang,
            loading = false
        )
    }

    fun setTafsirBrowseLanguage(language: String) {
        _tafsirBrowse.value = _tafsirBrowse.value.copy(language = language)
    }

    fun updateTafsirBrowseQuery(q: String) {
        _tafsirBrowse.value = _tafsirBrowse.value.copy(searchQuery = q)
    }

    fun loadFullQuran() = viewModelScope.launch {
        if (_fullQuran.value.items.isNotEmpty()) return@launch
        _fullQuran.value = FullQuranUiState(loading = true)

        val allAyat = repo.getAllAyat()
        val surahs = repo.getSurahList()
        val surahNames = surahs.associate { it.surahNumber to it.nameFa }
        val translations = repo.getAllTranslations(currentTranslationLanguage())
        val tafsirIds = repo.getAyahIdsWithTafsir()
        val juzList = repo.getJuzList()
        val allWords = repo.getAllWords()
        val wordsByAyah = allWords.groupBy { it.aId }

        val items = mutableListOf<ReadingItem>()
        val ayahItemIndex = HashMap<String, Int>()
        val surahItemIndex = HashMap<Int, Int>()
        var lastSurah = -1

        for (ayah in allAyat) {
            val surahName = surahNames[ayah.surahNumber] ?: ""
            val wordsForAyah = wordsByAyah[ayah.aId] ?: emptyList()
            val bismillahWords = wordsForAyah.filter { it.type == 6 }
            val mainWords = wordsForAyah.filter { it.type != 6 }

            if (ayah.surahNumber != lastSurah) {
                surahItemIndex[ayah.surahNumber] = items.size
                items += ReadingItem.SurahHeader(ayah.surahNumber, surahName)
                if (bismillahWords.isNotEmpty()) {
                    items += ReadingItem.Bismillah(ayah.surahNumber, bismillahWords)
                }
                lastSurah = ayah.surahNumber
            }
            ayahItemIndex[ayah.aId] = items.size
            items += ReadingItem.Ayah(ayah, surahName, mainWords)
        }

        val juzAyahIndex = juzList.associate { it.juzNumber to (ayahItemIndex[it.startAId] ?: 0) }

        _fullQuran.value = FullQuranUiState(
            items = items,
            translations = translations,
            ayahIdsWithTafsir = tafsirIds,
            bookmarkedAyahIds = progressRepo.getBookmarks().toSet(),
            ayahItemIndex = ayahItemIndex,
            surahItemIndex = surahItemIndex,
            juzAyahIndex = juzAyahIndex,
            showTranslation = _fullQuran.value.showTranslation,
            loading = false
        )
    }

    /** فقط یک‌بار در طول عمر برنامه: اگر آخرین محل مطالعه ذخیره شده، آیدی آن را برمی‌گرداند */
    fun consumeInitialScrollAyah(): String? {
        if (appliedInitialScroll) return null
        appliedInitialScroll = true
        return progressRepo.getLastReadAyah()
    }

    fun saveLastReadPosition(aId: String) {
        progressRepo.saveLastReadAyah(aId)
    }

    fun toggleBookmark(aId: String) {
        progressRepo.toggleBookmark(aId)
        _fullQuran.value = _fullQuran.value.copy(bookmarkedAyahIds = progressRepo.getBookmarks().toSet())
    }

    fun loadBookmarks() = viewModelScope.launch {
        _bookmarksScreen.value = BookmarksUiState(loading = true)
        val ids = progressRepo.getBookmarks()
        val ayat = repo.getAyahsByIds(ids)
        val sorted = ayat.sortedBy { it.aId }
        val surahs = repo.getSurahList()
        val names = surahs.associate { it.surahNumber to it.nameFa }
        _bookmarksScreen.value = BookmarksUiState(bookmarks = sorted, surahNames = names, loading = false)
    }

    fun requestScrollToAyah(aId: String) {
        _fullQuran.value.ayahItemIndex[aId]?.let { _scrollTarget.value = it }
    }

    private fun currentTranslationLanguage(): String =
        if (_settings.value.translationLanguage == "en") TR_LANG_EN else TR_LANG_FA

    /** وقتی زبان ترجمه عوض می‌شود، فقط نقشه‌ی ترجمه را دوباره می‌خواند (بدون بارگذاری مجدد کل قرآن) */
    fun refreshTranslations() = viewModelScope.launch {
        if (_fullQuran.value.items.isEmpty()) return@launch
        val translations = repo.getAllTranslations(currentTranslationLanguage())
        _fullQuran.value = _fullQuran.value.copy(translations = translations)
    }

    fun toggleFullQuranTranslationVisible() {
        _fullQuran.value = _fullQuran.value.copy(showTranslation = !_fullQuran.value.showTranslation)
    }

    fun requestScrollToSurah(surahNumber: Int) {
        _fullQuran.value.surahItemIndex[surahNumber]?.let { _scrollTarget.value = it }
    }

    fun requestScrollToJuz(juzNumber: Int) {
        _fullQuran.value.juzAyahIndex[juzNumber]?.let { _scrollTarget.value = it }
    }

    fun consumeScrollTarget() {
        _scrollTarget.value = null
    }

    /** قبل از رفتن به صفحه تفسیر، آیه جاری را ذخیره می‌کند تا هنگام بازگشت به همان‌جا اسکرول شود */
    fun rememberReturnAyah(aId: String) {
        pendingReturnAyahId = aId
    }

    /** هنگام ورود مجدد به صفحه اصلی (بازگشت از تفسیر) صدا زده می‌شود */
    fun consumePendingReturnAyah(): String? {
        val id = pendingReturnAyahId
        pendingReturnAyahId = null
        return id
    }

    fun itemIndexForAyah(aId: String): Int? = _fullQuran.value.ayahItemIndex[aId]

    fun loadSurahList() = viewModelScope.launch {
        _surahList.value = SurahListUiState(loading = true)
        val list = repo.getSurahList()
        _surahList.value = SurahListUiState(list, loading = false)
    }

    fun loadJuzList() = viewModelScope.launch {
        _juzList.value = JuzListUiState(loading = true)
        val list = repo.getJuzList()
        _juzList.value = JuzListUiState(list, loading = false)
    }

    fun setTafsirLanguage(language: String) {
        _tafsir.value = _tafsir.value.copy(language = language)
    }

    fun loadTafsir(aId: String, surahName: String, ayahNumber: Int) = viewModelScope.launch {
        val keepLang = _tafsir.value.language
        _tafsir.value = TafsirUiState(surahName = surahName, ayahNumber = ayahNumber, language = keepLang, loading = true)
        val entriesAr = repo.getTafsirForAyah(aId, "ar")
        val entriesFa = repo.getTafsirForAyah(aId, "fa")
        _tafsir.value = TafsirUiState(surahName, ayahNumber, entriesAr, entriesFa, keepLang, loading = false)
    }

    fun updateQuery(q: String) {
        _search.value = _search.value.copy(query = q)
    }

    fun toggleFilter(kind: String) {
        val s = _search.value
        _search.value = when (kind) {
            "quran" -> s.copy(includeQuran = !s.includeQuran)
            "translation" -> s.copy(includeTranslation = !s.includeTranslation)
            "tafsir" -> s.copy(includeTafsir = !s.includeTafsir)
            else -> s
        }
    }

    fun runSearch() = viewModelScope.launch {
        val s = _search.value
        if (s.query.isBlank()) {
            _search.value = s.copy(results = emptyList(), loading = false)
            return@launch
        }
        _search.value = s.copy(loading = true)
        val results = repo.search(
            s.query, s.includeQuran, s.includeTranslation, s.includeTafsir,
            translationLanguage = currentTranslationLanguage(),
            tafsirLanguage = TAFSIR_LANG_AR
        )
        if (_search.value.query == s.query) {
            _search.value = _search.value.copy(results = results, loading = false)
        }
    }
}
