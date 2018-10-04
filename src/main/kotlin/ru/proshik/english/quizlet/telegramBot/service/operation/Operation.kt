package ru.proshik.english.quizlet.telegramBot.service.operation

import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import ru.proshik.english.quizlet.telegramBot.model.Account
import ru.proshik.english.quizlet.telegramBot.service.vo.OperationData
import java.io.Serializable

interface Operation {

    fun init(chatId: Long, account: Account): BotApiMethod<out Serializable>

    fun execute(chatId: Long,
                messageId: Int,
                callData: String,
                value: OperationData,
                account: Account): BotApiMethod<out Serializable>

}

