package ru.proshik.english.quizlet.telegramBot.service.operation

import org.apache.log4j.Logger
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import ru.proshik.english.quizlet.telegramBot.service.AuthenticationService
import ru.proshik.english.quizlet.telegramBot.service.UsersService
import ru.proshik.english.quizlet.telegramBot.service.operation.OperationService.Operation.*
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap

class OperationService(private val usersService: UsersService,
                       private val authenticationService: AuthenticationService,
                       private val studiedOperation: StudiedOperation,
                       private val notificationOperation: NotificationOperation) {

    enum class Command(name: String) {
        START("/start"),
        HELP("/help"),
        CONNECT("/connect"),
        RECONNECT("/reconnect")
    }

    enum class Operation {
        STUDIED,
        NOTIFICATIONS
    }

    companion object {

        private val LOG = Logger.getLogger(OperationService::class.java)

        private val OPERATIONS = values().toList().map { operation -> operation.name }.toList()

        private const val DEFAULT_MESSAGE = "This bot can help you get information about studied sets on quizlet.com"
    }


    data class ActionOperation(val operation: Operation, val messageId: Int? = null)

    private val actionOperationStore = ConcurrentHashMap<Long, ActionOperation>()

    fun handleCommand(chatId: Long, command: Command): BotApiMethod<out Serializable> {
        return when (command) {
            Command.START -> SendMessage().setChatId(chatId).setText(DEFAULT_MESSAGE)
            Command.HELP -> SendMessage().setChatId(chatId).setText(DEFAULT_MESSAGE)
            Command.CONNECT -> SendMessage().setChatId(chatId).setText(connectToQuizlet(chatId))
            Command.RECONNECT -> SendMessage().setChatId(chatId).setText(authenticationService.generateAuthUrl(chatId.toString()))
        }
    }

    fun handleOperation(chatId: Long, text: String): BotApiMethod<out Serializable> {
        // text message is not operation
        if (!OPERATIONS.contains(text)) {
            val user = usersService.getUser(chatId.toString())
            return when {
                user != null && user.account != null ->
                    SendMessage().setChatId(chatId).setText("Select operation: ").setReplyMarkup(buildMainMenu())
                user != null && user.account == null ->
                    SendMessage().setChatId(chatId).setText(authenticationService.generateAuthUrl(chatId.toString()))
                else ->
                    SendMessage().setChatId(chatId).setText(connectToQuizlet(chatId))
            }
        }

        // text message is operation
        return when (valueOf(text)) {
            STUDIED -> {
                val (message, existData) = studiedOperation.init(chatId)

                if (existData)
                    actionOperationStore[chatId] = ActionOperation(STUDIED)
                message
            }
            NOTIFICATIONS -> {
                val (message, existData) = notificationOperation.init(chatId)

                if (existData)
                    actionOperationStore[chatId] = ActionOperation(NOTIFICATIONS)
                actionOperationStore[chatId] = ActionOperation(NOTIFICATIONS)
                message
            }
        }
    }

    fun handleCallback(chatId: Long, messageId: Int, callData: String): BotApiMethod<out Serializable> {
        val actionOperations = actionOperationStore[chatId]
        return if (actionOperations != null) {
            val (message, finalStep) = when (actionOperations.operation) {
                STUDIED -> studiedOperation.navigate(chatId, messageId, callData)
                NOTIFICATIONS -> studiedOperation.navigate(chatId, messageId, callData)
            }
            if (finalStep) actionOperationStore.remove(chatId)
            message
        } else
            EditMessageReplyMarkup().setChatId(chatId).setMessageId(messageId).setReplyMarkup(null)

    }

    private fun connectToQuizlet(chatId: Long): String {
        val user = usersService.getUser(chatId.toString())
        if (user == null) {
            usersService.create(chatId.toString())
        }

        return authenticationService.generateAuthUrl(chatId.toString())
    }

    private fun buildAuthMenu(): ReplyKeyboardMarkup {
        val keyboardMarkup = ReplyKeyboardMarkup().apply {
            resizeKeyboard = true
            selective = true
        }

        val rows = ArrayList<KeyboardRow>()
        val row = KeyboardRow()
        row.add("Authorize")
        rows.add(row)

        keyboardMarkup.keyboard = rows

        return keyboardMarkup
    }

    private fun buildMainMenu(): ReplyKeyboardMarkup {
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


}