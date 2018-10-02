package ru.proshik.english.quizlet.telegramBot.service

import org.apache.log4j.Logger
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import ru.proshik.english.quizlet.telegramBot.service.TelegramBotHandler.GreetingsMenu.AUTHORIZE
import ru.proshik.english.quizlet.telegramBot.service.TelegramBotHandler.MainMenu.NOTIFICATIONS
import ru.proshik.english.quizlet.telegramBot.service.TelegramBotHandler.MainMenu.STUDIED
import ru.proshik.english.quizlet.telegramBot.service.TelegramBotHandler.NotificationMenu.*
import ru.proshik.english.quizlet.telegramBot.service.model.CommandType
import ru.proshik.english.quizlet.telegramBot.service.operation.NotificationOperation
import ru.proshik.english.quizlet.telegramBot.service.operation.StudiedOperation
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap

@Component
class TelegramBotHandler(private val accountService: AccountService,
                         private val authenticationService: AuthenticationService,
                         private val studiedOperation: StudiedOperation,
                         private val notificationOperation: NotificationOperation) {

    enum class GreetingsMenu(val title: String) {
        AUTHORIZE("Connect to Quizlet.com");
    }

    enum class MainMenu(val title: String) {
        STUDIED("Studied"),
        NOTIFICATIONS("Notifications");
    }

    enum class NotificationMenu(val title: String) {
        REMINDING("Reminding about doesn't studied sets"),
        //        ANNOUNCEMENT("Announcement new available"),
        MAIN_MENU("Main menu");
    }

    companion object {

        private val LOG = Logger.getLogger(TelegramBotHandler::class.java)

        private const val DEFAULT_MESSAGE = "The bot will help you to get information about studied sets on https://quizlet.com"
        private const val AUTHORIZE_URL = "Please, use the URL to authorize with quizlet's site:"
        private const val COMMAND_NOT_FOUND = "Command doesn't exist."

        fun buildMainMenu(): ReplyKeyboardMarkup {
            return buildMenu(MainMenu.values().map { it.title })
        }

        fun buildAuthorizeMenu(): ReplyKeyboardMarkup {
            return buildMenu(listOf(AUTHORIZE.title))
        }

        fun buildNotificationMenu(): ReplyKeyboardMarkup {
            return buildMenu(values().map { it.title })
        }

        private fun buildMenu(titles: List<String>): ReplyKeyboardMarkup {
            val keyboardMarkup = ReplyKeyboardMarkup().apply {
                resizeKeyboard = true
                selective = true
            }

            val rows = ArrayList<KeyboardRow>()
            for (title in titles) {
                val row = KeyboardRow()
                row.add(title)
                rows.add(row)
            }

            keyboardMarkup.keyboard = rows

            return keyboardMarkup
        }
    }

    data class ActionOperation(val operation: MainMenu, val messageId: Int? = null)

    private val actionOperationStore = ConcurrentHashMap<Long, ActionOperation>()

    @Transactional
    fun handleCommand(chatId: Long, text: String): BotApiMethod<out Serializable> {
        val account = accountService.getAccount(chatId)

        val commandType = CommandType.getByName(text) ?: return buildMessage(chatId, COMMAND_NOT_FOUND)

        return when (commandType) {
            CommandType.START, CommandType.HELP ->
                buildMessage(chatId, DEFAULT_MESSAGE, if (account != null) buildMainMenu() else buildAuthorizeMenu())
            CommandType.AUTHORIZE, CommandType.RE_AUTHORIZE -> {
                val authUrl = authenticationService.connectToQuizlet(chatId)
                LOG.info("user with chatId=$chatId was authorized/reauthorized")

                buildMessage(chatId, "$AUTHORIZE_URL $authUrl")
            }
            CommandType.REVOKE_AUTH -> {
                if (account != null) {
                    accountService.deleteAccount(account.id)

                    LOG.info("access token was revoked for user with chatId=$chatId")

                    buildMessage(chatId, "Account ${account.login} was delete", buildAuthorizeMenu())
                } else {
                    buildMessage(chatId, "Account doesn't exist", buildAuthorizeMenu())
                }
            }
        }
    }

    @Transactional
    fun handleOperation(chatId: Long, messageId: Int, text: String): BotApiMethod<out Serializable> {
        val account = accountService.getAccount(chatId)

        return when (text) {
            AUTHORIZE.title -> {
                val authUrl = authenticationService.connectToQuizlet(chatId)
                buildMessage(chatId, "$AUTHORIZE_URL $authUrl", buildAuthorizeMenu())
            }
            STUDIED.title -> {
                val (message, existData) = studiedOperation.init(chatId)

                if (existData)
                    actionOperationStore[chatId] = ActionOperation(STUDIED)
                message
            }
            NOTIFICATIONS.title -> buildMessage(chatId, "Select a notification type: ", buildNotificationMenu())
            // TODO fix it after implement
            REMINDING.title -> return notificationOperation.init(chatId).message
            MAIN_MENU.title -> buildMessage(chatId, "Please, use a keyboard", buildMainMenu())
            else -> buildMessage(
                    chatId,
                    "Unknown operation. Please, use a keyboard buttons",
                    if (account != null) buildMainMenu() else buildAuthorizeMenu(),
                    messageId)
        }
    }

    @Transactional
    fun handleCallback(chatId: Long, messageId: Int, callData: String): BotApiMethod<out Serializable> {
        val account = accountService.getAccount(chatId) ?: return SendMessage()
                .setChatId(chatId)
                .setText("Please, authorize with quizlet.com, use the screen keyboard")
                .setReplyMarkup(buildAuthorizeMenu())

        val actionOperations = actionOperationStore[chatId]
        return if (actionOperations != null) {
            return when (actionOperations.operation) {
                STUDIED -> studiedOperation.navigate(chatId, messageId, callData)
                NOTIFICATIONS -> studiedOperation.navigate(chatId, messageId, callData)
            }
        } else
            EditMessageReplyMarkup().setChatId(chatId).setMessageId(messageId).setReplyMarkup(null)

    }

    private fun buildMessage(chatId: Long,
                             text: String,
                             keyboard: ReplyKeyboardMarkup? = null,
                             messageId: Int? = null): BotApiMethod<out Serializable> {
        val message = SendMessage()
                .setChatId(chatId)
                .setText(text)

        if (keyboard != null) message.replyMarkup = keyboard
        if (messageId != null) message.replyToMessageId = messageId

        return message
    }

}