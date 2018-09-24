package ru.proshik.english.quizlet.telegramBot.service.operation

enum class Command(name: String) {

    START("/start"),
    HELP("/help"),
    CONNECT("/connect"),
    RECONNECT("/reconnect")

}