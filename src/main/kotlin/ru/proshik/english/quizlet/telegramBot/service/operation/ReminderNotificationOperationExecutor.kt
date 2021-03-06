package ru.proshik.english.quizlet.telegramBot.service.operation

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import ru.proshik.english.quizlet.telegramBot.service.vo.NavigateType
import java.io.Serializable

@Component
class ReminderNotificationOperationExecutor : OperationExecutor {

    enum class StepType {
        ACTION,
        ADD_DAY_OF_WEEK,
        ADD_HOUR_OF_DAY,
        ADD_RESULT,
        DELETE_
    }

    override fun init(chatId: Long): BotApiMethod<out Serializable> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun execute(chatId: Long,
                         messageId: Int,
                         stepType: String,
                         navType: NavigateType,
                         value: String): BotApiMethod<out Serializable> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}