package com.linqingying.cangjie.configurable

import java.util.*

enum class LanguageOption(val locale: Locale, val displayName: String) {
    CHINESE(Locale.SIMPLIFIED_CHINESE, "简体中文"),

    ENGLISH(Locale.ENGLISH, "English") ;

    override fun toString(): String {
        return displayName
    }
    companion object {
        fun fromLocale(locale: Locale): LanguageOption {
            return entries.firstOrNull { it.locale == locale } ?: CHINESE
        }

        fun fromDisplayName(displayName: String): LanguageOption {
            return entries.firstOrNull { it.displayName == displayName } ?: CHINESE
        }


    }
}
