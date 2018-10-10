package ru.proshik.english.quizlet.telegramBot.service.vo

enum class CommandType(val title: String) {

    START("start"),
    HELP("help"),
    AUTHORIZE("authorize"),
    REVOKE_AUTH("revoke");
//    STUDY("/stydy"),
//    NOTIFICATION("/notification");

    companion object {

        fun getByName(text: String): CommandType? = values().firstOrNull { it.title == text }
    }

}
