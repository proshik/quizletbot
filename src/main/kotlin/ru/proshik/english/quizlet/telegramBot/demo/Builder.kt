package ru.proshik.english.quizlet.telegramBot.demo

import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message

import java.io.Serializable

import java.lang.StrictMath.toIntExact

object Builder {

    fun bild(): BotApiMethod<out Serializable> {
        return EditMessageText()
    }

}
