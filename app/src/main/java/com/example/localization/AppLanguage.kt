package com.example.localization

/**
 * Unterstützte Sprachen der Anwendung mit Flaggen-Symbol und RTL-Flag.
 */
enum class AppLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val flagEmoji: String,
    val isRtl: Boolean = false
) {
    DE("de", "Deutsch", "German", "🇩🇪", isRtl = false),
    EN("en", "English", "English", "🇬🇧", isRtl = false),
    KU("ku", "Kurdî (Kurmancî)", "Kurdish", "☀️", isRtl = false),
    TR("tr", "Türkçe", "Turkish", "🇹🇷", isRtl = false),
    AR("ar", "العربية", "Arabic", "🇸🇦", isRtl = true),
    FR("fr", "Français", "French", "🇫🇷", isRtl = false),
    ES("es", "Español", "Spanish", "🇪🇸", isRtl = false),
    RU("ru", "Русский", "Russian", "🇷🇺", isRtl = false),
    ZH("zh", "简体中文", "Chinese", "🇨🇳", isRtl = false),
    HI("hi", "हिन्दी", "Hindi", "🇮🇳", isRtl = false);

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: DE
        }
    }
}
