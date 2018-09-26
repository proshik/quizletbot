package ru.proshik.english.quizlet.telegramBot.service.operation

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import java.io.Serializable

@Component
class NotificationOperation : Operation {

    override fun init(chatId: Long): InitResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun navigate(chatId: Long, messageId: Int, callData: String): StepResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}