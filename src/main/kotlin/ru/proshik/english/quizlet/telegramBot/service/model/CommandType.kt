package ru.proshik.english.quizlet.telegramBot.service.model

enum class CommandType(val title: String) {

    START("/start"),
    HELP("/help"),
    AUTHORIZE("/authorize"),
    RE_AUTHORIZE("/re-authorize");

    companion object {

        fun getByName(text: String): CommandType? = values().firstOrNull { it.title == text }

    }

}
