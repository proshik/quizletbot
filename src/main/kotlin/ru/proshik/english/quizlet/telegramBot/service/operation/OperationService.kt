package ru.proshik.english.quizlet.telegramBot.service.operation

import org.apache.log4j.Logger
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import ru.proshik.english.quizlet.telegramBot.service.AuthenticationService
import ru.proshik.english.quizlet.telegramBot.service.UsersService
import ru.proshik.english.quizlet.telegramBot.service.action.MessageFormatter
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap

class OperationService(private val usersService: UsersService,
                       private val authenticationService: AuthenticationService,
                       private val studiedOperation: StudiedOperation) {

    companion object {

        private val LOG = Logger.getLogger(OperationService::class.java)

        private val OPERATIONS = OperationType.values().toList().map { operation -> operation.name }.toList()

        private const val DEFAULT_MESSAGE = "This bot can help you get information about studied sets on quizlet.com"
    }

    val messageFormatter = MessageFormatter()

    val operationData = ConcurrentHashMap<Long, OperationPipeline>()

    fun handleCommand(chatId: Long, command: Command): BotApiMethod<out Serializable> {
        return when (command) {
            Command.START -> SendMessage().setChatId(chatId).setText(DEFAULT_MESSAGE)
            Command.HELP -> SendMessage().setChatId(chatId).setText(DEFAULT_MESSAGE)
            Command.CONNECT -> SendMessage().setChatId(chatId).setText(connectToQuizlet(chatId))
            Command.RECONNECT -> SendMessage().setChatId(chatId).setText(authenticationService.generateAuthUrl(chatId.toString()))
        }
    }

    fun handleOperation(chatId: Long, text: String): BotApiMethod<out Serializable> {
        return if (OPERATIONS.contains(text)) {
            val operationStep = operationRunner(chatId, OperationType.valueOf(text))
            if (operationStep == null) {
                SendMessage().setChatId(chatId).setText("Data doesn't find")
            } else {
                operationData[chatId] = operationStep.pipeline
//                messageFormatter.editMessageInlineKeyboard()
                TODO()
            }
        } else {
            val user = usersService.getUser(chatId.toString())
            return if (user != null && user.account != null) {
                SendMessage().setChatId(chatId).setText("Select operation: ").setReplyMarkup(buildMenuKeyboard())
            } else if (user != null && user.account == null) {
                SendMessage().setChatId(chatId).setText(authenticationService.generateAuthUrl(chatId.toString()))
            } else {
                SendMessage().setChatId(chatId).setText(connectToQuizlet(chatId))
            }
        }
    }

    fun handleCallback(chatId: Long, messageId: Int, callData: String): BotApiMethod<out Serializable> {
        operationData[chatId] = OperationPipeline(OperationType.NOTIFICATIONS, StudiedOperation.StudiedOperationStep.SELECT_GROUP, HashMap())
        val data = operationData[chatId]
        return if (data != null) {
            when (data.type) {
                OperationType.STUDIED -> studiedOperation.nextSubOperation(chatId, messageId, callData, data)
                OperationType.NOTIFICATIONS -> notImplementMessage(chatId)
            }
        } else {
            EditMessageReplyMarkup().setChatId(chatId).setMessageId(messageId).setReplyMarkup(null)
        }
    }

    private fun connectToQuizlet(chatId: Long): String {
        val user = usersService.getUser(chatId.toString())
        if (user == null) {
            usersService.create(chatId.toString())
        }

        return authenticationService.generateAuthUrl(chatId.toString())
    }

    private fun buildMenuKeyboard(): ReplyKeyboardMarkup {
        val keyboardMarkup = ReplyKeyboardMarkup().apply {
            resizeKeyboard = true
            selective = true
        }

        val rows = ArrayList<KeyboardRow>()
        for (operation in OPERATIONS) {
            val row = KeyboardRow()
            row.add(operation)
            rows.add(row)
        }

        keyboardMarkup.keyboard = rows

        return keyboardMarkup
    }

    private fun operationRunner(chatId: Long, operationType: OperationType): OperationStepInfo? {
        return when (operationType) {
            OperationType.STUDIED -> studiedOperation.initOperation(chatId)
            OperationType.NOTIFICATIONS -> null
        }
    }

    private fun notImplementMessage(chatId: Long): BotApiMethod<out Serializable> {
        return SendMessage().setChatId(chatId).setText("Development in process")
    }

}