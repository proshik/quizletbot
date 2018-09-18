package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import ru.proshik.english.quizlet.telegramBot.service.operation.Operation
import ru.proshik.english.quizlet.telegramBot.service.operation.StaticOperationProvider
import ru.proshik.english.quizlet.telegramBot.service.operation.StatisticOperation
import java.util.concurrent.ConcurrentHashMap

@Service
class QuizletOperationService(private val usersService: UsersService,
                              private val authenticationService: AuthenticationService,
                              private val quizletInfoService: QuizletInfoService) {

    companion object {
        private const val STATISTIC_COMMAND = "Statistics"
        private const val NOTIFICATIONS_COMMAND = "Notifications"
        private const val ACCOUNT_COMMAND = "Account"

        private val COMMANDS = listOf(STATISTIC_COMMAND, NOTIFICATIONS_COMMAND, ACCOUNT_COMMAND)
    }

    // TODO when I will read about generict in the kotlin I change generic types
    val operationQueue = ConcurrentHashMap<Long, Operation<*, *>>()

    fun connectToQuizlet(chatId: Long): String {
        val user = usersService.getUser(chatId.toString())
        if (user == null) {
            usersService.create(chatId.toString())
        }

        return authenticationService.generateAuthUrl(chatId.toString())
    }

    fun handleOperation(chatId: Long, text: String): BotApiMethod<Message> {
        val operation = operationQueue[chatId]

        if (operation != null) {
            val operationHandle = operation.nextStep(text)
            // TODO remove cast after transform to kotlin code
            return operation.formatter.format(operationHandle)
        }

        val operationEvent = operationResolver(chatId, text)
        if (operationEvent == null) {
            // TODO change text
            return SendMessage().setChatId(chatId).setText("Default message like \"use the keyboard and bla-bla-bla\"")
        } else {
            val initResult = operationEvent.init()

            return operationEvent.formatter.format(initResult)
        }
    }

    private fun operationResolver(chatId: Long, text: String): Operation<*, *>? {
        return when (text) {
            // TODO exclude StaticOperationProvider from Operation. Need to do a operation as light as possible
            STATISTIC_COMMAND -> StatisticOperation(chatId, StaticOperationProvider(quizletInfoService))
            else -> null
        }
    }

}
