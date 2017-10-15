package utils

import main.httpClient
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import web.LangModel

val languages = listOf(
        LangModel("English", "en"),
        LangModel("French", "fr"),
        LangModel("Spanish", "es"),
        LangModel("Russian", "ru"),
        LangModel("Turkish", "tr"),
        LangModel("Ukranian", "uk"),
        LangModel("English", "en"),
        LangModel("Swahili", "sw"),
        LangModel("Azerbaijani", "az"),
        LangModel("Armenian", "hy"),
        LangModel("Indonesian", "id"),
        LangModel("Korean", "ko"),
        LangModel("Belarusian", "be"),
        LangModel("Japanese", "ja"),
        LangModel("Georgian", "ka"),
        LangModel("Kazakh", "kk"),
        LangModel("Italian", "it"),
        LangModel("Romanian", "ro"),
        LangModel("Hungarian", "hu"),
        LangModel("Malay", "ms"),
        LangModel("Macedonian", "mk"),
        LangModel("Persian", "fa"),
        LangModel("Danish", "da"),
        LangModel("Latvian", "lv"),
        LangModel("Irish", "ga"),
        LangModel("Serbian", "sr"),
        LangModel("Albanian", "sq"),
        LangModel("Polish", "pl"),
        LangModel("Croatian", "hr"),
        LangModel("Thai", "th"),
        LangModel("Norwegian", "no"),
        LangModel("Finnish", "fi"),
        LangModel("Tajik", "tg"),
        LangModel("Basque", "eu"),
        LangModel("Arabic", "ar"),
        LangModel("Catalan", "ca"),
        LangModel("Dutch", "nl"),
        LangModel("Bulgarian", "bg"),
        LangModel("Afrikaans", "af"),
        LangModel("Mongolian", "mn"),
        LangModel("Haitian", "ht"),
        LangModel("Portuguese", "pt"),
        LangModel("German", "de"),
        LangModel("Tagalog", "tl"),
        LangModel("Bosnian", "bs"),
        LangModel("Vietnamese", "vi"),
        LangModel("Czech", "cs"),
        LangModel("Greek", "el"),
        LangModel("Malagasy", "mg"),
        LangModel("Slovak", "sk"),
        LangModel("Lithuanian", "lt"),
        LangModel("Estonian", "et"),
        LangModel("Chinese", "zh"),
        LangModel("Hebrew", "he"),
        LangModel("Uzbek", "uz"),
        LangModel("Latin", "la"),
        LangModel("Slovenian", "sl"),
        LangModel("Swedish", "sv"),
        LangModel("Icelandic", "is")
)

val languages_table_one = languages.subList(0, languages.size / 2)
val languages_table_two = languages.subList(languages.size / 2, languages.size)