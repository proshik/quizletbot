package ru.proshik.english.quizlet.telegramBot.service.action

data class InlineKeyboardData(val text: String,
                              val items: List<Pair<String, String>>,
                              val messageId: Int? = null)
