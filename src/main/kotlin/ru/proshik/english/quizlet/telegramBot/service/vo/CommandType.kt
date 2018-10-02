package ru.proshik.english.quizlet.telegramBot.service.vo

enum class CommandType(val title: String) {

    START("/start"),
    HELP("/help"),
    AUTHORIZE("/auth"),
    RE_AUTHORIZE("/re-auth"),
    REVOKE_AUTH("/revoke-auth");

    companion object {
        fun getByName(text: String): CommandType? = values().firstOrNull { it.title == text }
    }

}
