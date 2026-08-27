package com.lbo.quran.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lbo.quran.ui.theme.QuranFontOptions
import com.lbo.quran.ui.theme.TranslationFontOptions
import com.lbo.quran.ui.theme.quranFontByKey
import com.lbo.quran.ui.theme.translationFontByKey

private val TEXT_COLOR_SWATCHES = listOf(
    0xFF1B1B1B, 0xFFFFFFFF, 0xFF5B3A29, 0xFF7A1F1F,
    0xFF1F4B2F, 0xFF1F3A5F, 0xFF5A1F5A, 0xFFB8860B
)

private val BACKGROUND_COLOR_SWATCHES = listOf(
    0xFFFFFFFF, 0xFFFBF3E0, 0xFFF3EFE6, 0xFFEFF6EE,
    0xFFEAF1F8, 0xFF1B1B1B, 0xFF262626, 0xFF0F2A1C
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات نمایش") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("متن قرآن", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            FontDropdown(
                options = QuranFontOptions,
                selectedKey = settings.quranFontKey,
                onSelect = { viewModel.updateSettings(settings.copy(quranFontKey = it)) }
            )

            Spacer(Modifier.height(12.dp))
            Text("اندازه قلم: ${settings.quranFontSize.toInt()}")
            Slider(
                value = settings.quranFontSize,
                onValueChange = { viewModel.updateSettings(settings.copy(quranFontSize = it)) },
                valueRange = 16f..40f,
                steps = 11
            )

            Spacer(Modifier.height(12.dp))
            Text("رنگ متن", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            ColorSwatchRow(
                swatches = TEXT_COLOR_SWATCHES,
                selected = settings.quranTextColor,
                onSelect = { viewModel.updateSettings(settings.copy(quranTextColor = it)) }
            )

            Spacer(Modifier.height(12.dp))
            Text("رنگ پس‌زمینه", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            ColorSwatchRow(
                swatches = BACKGROUND_COLOR_SWATCHES,
                selected = settings.quranBackgroundColor,
                onSelect = { viewModel.updateSettings(settings.copy(quranBackgroundColor = it)) }
            )

            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(settings.quranBackgroundColor))) {
                Text(
                    "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    fontFamily = quranFontByKey(settings.quranFontKey),
                    fontSize = settings.quranFontSize.sp,
                    color = Color(settings.quranTextColor),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Text("متن ترجمه و تفسیر", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            FontDropdown(
                options = TranslationFontOptions,
                selectedKey = settings.translationFontKey,
                onSelect = { viewModel.updateSettings(settings.copy(translationFontKey = it)) }
            )

            Spacer(Modifier.height(12.dp))
            Text("اندازه قلم: ${settings.translationFontSize.toInt()}")
            Slider(
                value = settings.translationFontSize,
                onValueChange = { viewModel.updateSettings(settings.copy(translationFontSize = it)) },
                valueRange = 12f..28f,
                steps = 15
            )

            Spacer(Modifier.height(12.dp))
            Text("رنگ متن تفسیر", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            ColorSwatchRow(
                swatches = TEXT_COLOR_SWATCHES,
                selected = settings.tafsirTextColor,
                onSelect = { viewModel.updateSettings(settings.copy(tafsirTextColor = it)) }
            )

            Spacer(Modifier.height(12.dp))
            Text("رنگ پس‌زمینه تفسیر", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            ColorSwatchRow(
                swatches = BACKGROUND_COLOR_SWATCHES,
                selected = settings.tafsirBackgroundColor,
                onSelect = { viewModel.updateSettings(settings.copy(tafsirBackgroundColor = it)) }
            )

            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(settings.tafsirBackgroundColor))) {
                Text(
                    "این متن نمونه‌ای از رنگ و فونت انتخابی برای ترجمه و تفسیر است.",
                    fontFamily = translationFontByKey(settings.translationFontKey),
                    fontSize = settings.translationFontSize.sp,
                    color = Color(settings.tafsirTextColor),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ColorSwatchRow(
    swatches: List<Long>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        swatches.forEach { swatchLong ->
            val argb = swatchLong.toInt()
            val isSelected = argb == selected
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(argb))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFBBBBBB),
                        shape = CircleShape
                    )
                    .clickable { onSelect(argb) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    val iconTint = if (Color(argb).luminance() > 0.5f) Color.Black else Color.White
                    Icon(Icons.Default.Check, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontDropdown(
    options: List<com.lbo.quran.ui.theme.FontOption>,
    selectedKey: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.key == selectedKey }?.label ?: options.first().label

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("فونت") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label, fontFamily = option.family) },
                    onClick = {
                        onSelect(option.key)
                        expanded = false
                    }
                )
            }
        }
    }
}
