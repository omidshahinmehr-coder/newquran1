package com.lbo.quran.data

import android.content.Context

data class AppSettings(
    val quranFontKey: String = "taha",
    val quranFontSize: Float = 22f,
    val translationFontKey: String = "estedad",
    val translationFontSize: Float = 16f,
    val translationLanguage: String = "fa", // "fa" or "en"
    val quranTextColor: Int = 0xFF1B1B1B.toInt(),
    val quranBackgroundColor: Int = 0xFFFFFFFF.toInt(),
    val tafsirTextColor: Int = 0xFF1B1B1B.toInt(),
    val tafsirBackgroundColor: Int = 0xFFFFFFFF.toInt()
)

class SettingsRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("quran_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        quranFontKey = prefs.getString(KEY_QURAN_FONT, "taha") ?: "taha",
        quranFontSize = prefs.getFloat(KEY_QURAN_SIZE, 22f),
        translationFontKey = prefs.getString(KEY_TR_FONT, "estedad") ?: "estedad",
        translationFontSize = prefs.getFloat(KEY_TR_SIZE, 16f),
        translationLanguage = prefs.getString(KEY_TR_LANG, "fa") ?: "fa",
        quranTextColor = prefs.getInt(KEY_QURAN_TEXT_COLOR, 0xFF1B1B1B.toInt()),
        quranBackgroundColor = prefs.getInt(KEY_QURAN_BG_COLOR, 0xFFFFFFFF.toInt()),
        tafsirTextColor = prefs.getInt(KEY_TAFSIR_TEXT_COLOR, 0xFF1B1B1B.toInt()),
        tafsirBackgroundColor = prefs.getInt(KEY_TAFSIR_BG_COLOR, 0xFFFFFFFF.toInt())
    )

    fun save(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_QURAN_FONT, settings.quranFontKey)
            .putFloat(KEY_QURAN_SIZE, settings.quranFontSize)
            .putString(KEY_TR_FONT, settings.translationFontKey)
            .putFloat(KEY_TR_SIZE, settings.translationFontSize)
            .putString(KEY_TR_LANG, settings.translationLanguage)
            .putInt(KEY_QURAN_TEXT_COLOR, settings.quranTextColor)
            .putInt(KEY_QURAN_BG_COLOR, settings.quranBackgroundColor)
            .putInt(KEY_TAFSIR_TEXT_COLOR, settings.tafsirTextColor)
            .putInt(KEY_TAFSIR_BG_COLOR, settings.tafsirBackgroundColor)
            .apply()
    }

    companion object {
        private const val KEY_QURAN_FONT = "quran_font_key"
        private const val KEY_QURAN_SIZE = "quran_font_size"
        private const val KEY_TR_FONT = "translation_font_key"
        private const val KEY_TR_SIZE = "translation_font_size"
        private const val KEY_TR_LANG = "translation_language"
        private const val KEY_QURAN_TEXT_COLOR = "quran_text_color"
        private const val KEY_QURAN_BG_COLOR = "quran_bg_color"
        private const val KEY_TAFSIR_TEXT_COLOR = "tafsir_text_color"
        private const val KEY_TAFSIR_BG_COLOR = "tafsir_bg_color"
    }
}
