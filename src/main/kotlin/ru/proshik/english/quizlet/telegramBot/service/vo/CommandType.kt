package ru.proshik.english.quizlet.telegramBot.service.vo

enum class CommandType(val title: String) {

    START("/start"),
    HELP("/help"),
    AUTHORIZE("/auth"),
    RE_AUTHORIZE("/re_auth"),
    REVOKE_AUTH("/revoke_auth");
//    STUDY("/stydy"),
//    NOTIFICATION("/notification");

    companion object {

        fun getByName(text: String): CommandType? = values().firstOrNull { it.title == text }
    }

}
