package ru.proshik.english.quizlet.telegramBot.service.operation

import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import ru.proshik.english.quizlet.telegramBot.model.Users
import java.io.Serializable

interface OperationExecutor {

    fun init(chatId: Long, user: Users): BotApiMethod<out Serializable>

    fun execute(chatId: Long,
                messageId: Int,
                callData: String,
                user: Users): BotApiMethod<out Serializable>

}

