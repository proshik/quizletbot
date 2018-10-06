package ru.proshik.english.quizlet.telegramBot.service.vo

data class UserCredentials(val login: String, val accessToken: String)

//private val userContext: (user: Users) -> UserCredentials = { UserCredentials(it.login, it.accessToken) }