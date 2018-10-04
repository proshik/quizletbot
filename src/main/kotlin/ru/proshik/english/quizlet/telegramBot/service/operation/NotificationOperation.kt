package ru.proshik.english.quizlet.telegramBot.service.operation

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import ru.proshik.english.quizlet.telegramBot.model.Account
import ru.proshik.english.quizlet.telegramBot.service.vo.OperationData
import java.io.Serializable

@Component
class NotificationOperation : Operation {

    enum class StepType {
        ACTION,
        DAY_OF_WEEK,
        HOUR_OF_DAY
    }

    override fun init(chatId: Long, account: Account): BotApiMethod<out Serializable> {
        return SendMessage().setChatId(chatId).setText("OperationData doesn't implement")
    }

    override fun execute(chatId: Long, messageId: Int, callData: String, value: OperationData, account: Account): BotApiMethod<out Serializable> {
        return SendMessage().setChatId(chatId).setText("OperationData doesn't implement")
    }

}