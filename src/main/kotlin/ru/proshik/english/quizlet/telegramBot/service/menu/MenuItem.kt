package ru.proshik.english.quizlet.telegramBot.service.menu

data class MenuItem(val title: String, val items: List<MenuItem>? = emptyList())