package ru.proshik.english.quizlet.telegramBot.service.action

import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import ru.proshik.english.quizlet.telegramBot.service.operation.OperationStep
import java.io.Serializable


interface Action{

    fun init(chatId: Long): BotApiMethod<out Serializable>

    fun nextStep(chatId: Long, messageId: Int, callData: String, step: OperationStep, data: Map<String, String>): BotApiMethod<out Serializable>

}