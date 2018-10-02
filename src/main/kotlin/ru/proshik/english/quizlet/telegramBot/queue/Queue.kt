package ru.proshik.english.quizlet.telegramBot.queue

interface Queue {

    data class Message(val chatId: Long, val text: String)

    fun put(message: Message)

    fun take(): Message
}
