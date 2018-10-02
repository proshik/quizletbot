package ru.proshik.english.quizlet.telegramBot.service.operation

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import java.io.Serializable

@Component
class NotificationOperation : Operation {

    enum class StepType {
        ACTION,
        DAY_OF_WEEK,
        HOUR_OF_DAY
    }

    override fun init(chatId: Long): InitResult {
        return InitResult(SendMessage().setChatId(chatId).setText("Operation doesn't implement"), false)
    }

    override fun navigate(chatId: Long, messageId: Int, callData: String): BotApiMethod<out Serializable> {
        return SendMessage().setChatId(chatId).setText("Operation doesn't implement")
    }

}