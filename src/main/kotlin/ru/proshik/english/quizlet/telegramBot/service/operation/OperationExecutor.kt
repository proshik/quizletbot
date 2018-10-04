package ru.proshik.english.quizlet.telegramBot.service.operation

import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import ru.proshik.english.quizlet.telegramBot.model.User
import java.io.Serializable

interface OperationExecutor {

    fun init(chatId: Long, user: User): BotApiMethod<out Serializable>

    fun execute(chatId: Long,
                messageId: Int,
                callData: String,
                user: User): BotApiMethod<out Serializable>

}

