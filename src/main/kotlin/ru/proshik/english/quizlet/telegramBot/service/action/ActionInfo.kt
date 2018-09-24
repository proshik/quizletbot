package ru.proshik.english.quizlet.telegramBot.service.action

interface ActionInfo {

    fun getStep(): Step

    fun getParam(): String

    fun getParams(): List<String>

}