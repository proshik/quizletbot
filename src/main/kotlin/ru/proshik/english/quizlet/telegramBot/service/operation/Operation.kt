package ru.proshik.english.quizlet.telegramBot.service.operation

import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import java.io.Serializable

data class StepResult(val message: BotApiMethod<out Serializable>, val finalStep: Boolean)

data class InitResult(val message: BotApiMethod<out Serializable>, val existData: Boolean)

interface Operation {

    fun init(chatId: Long): InitResult

    fun navigate(chatId: Long, messageId: Int, callData: String): StepResult

}

