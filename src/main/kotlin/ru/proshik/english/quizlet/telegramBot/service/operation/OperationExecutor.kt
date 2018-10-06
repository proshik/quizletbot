package ru.proshik.english.quizlet.telegramBot.service.operation

import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import ru.proshik.english.quizlet.telegramBot.service.vo.NavigateType
import java.io.Serializable

interface OperationExecutor {

    fun init(chatId: Long): BotApiMethod<out Serializable>

    fun execute(chatId: Long,
                messageId: Int,
                stepType: String,
                navType: NavigateType,
                value: String): BotApiMethod<out Serializable>

}

