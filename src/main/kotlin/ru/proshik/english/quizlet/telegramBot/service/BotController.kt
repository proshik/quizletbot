package ru.proshik.english.quizlet.telegramBot.service

import org.apache.log4j.Logger
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import ru.proshik.english.quizlet.telegramBot.service.BotController.GreetingsMenu.AUTHORIZE
import ru.proshik.english.quizlet.telegramBot.service.BotController.MainMenu.*
import ru.proshik.english.quizlet.telegramBot.service.BotController.NotificationMenu.*
import ru.proshik.english.quizlet.telegramBot.service.operation.StudyOperationExecutor
import ru.proshik.english.quizlet.telegramBot.service.vo.CommandType
import ru.proshik.english.quizlet.telegramBot.service.vo.NavigateType
import ru.proshik.english.quizlet.telegramBot.service.vo.OperationType
import java.io.Serializable

@Component
class BotController(private val studyOperation: StudyOperationExecutor,
                    private val authorizationService: AuthorizationService,
                    private val userService: UserService) {

    // TODO implement menu as a Tree structure
    enum class GreetingsMenu(val title: String) {
        AUTHORIZE("Connect to Quizlet.com");
    }

    enum class MainMenu(val title: String) {
        STUDIED("\uD83D\uDCCA Your Study Sets"),
        NOTIFICATIONS("\uD83D\uDCEC Notifications"),
        ACCOUNT("👤 Account");

    }

    enum class NotificationMenu(val title: String) {
        REMINDING("Reminding about doesn't studied sets"),
        ANNOUNCEMENT("\uD83D\uDCE9 New sets available"),
        MAIN_MENU("Main menu");
    }

    companion object {

        private val LOG = Logger.getLogger(BotController::class.java)

        private const val DEFAULT_MESSAGE = "The QuizletBot will help you to get information about studied sets on Quizlet!"
        private const val SEIZE_TO_AUTHORIZE = "Please, seize the link to authorize:"
        private const val INVITATION_TO_AUTHORIZE = "Please, authorize on quizlet.com, use the screen keyboard."
        private const val ALREADY_AUTHORIZED = "You've already authorized with Quizlet!"
        private const val COMMAND_NOT_FOUND = "Command doesn't exist."
        private const val OPERATION_DOES_NOT_IMPLEMENT = "Sorry, operation haven't implemented yet! We are working on that! ⛏"
        private const val MAIN_MENU_MESSAGE = "You are on the Main menu! Please, use a screen keyboard."
        private const val REVOKE_ACCESS_TOKEN_MESSAGE = "Access token successfully revoke! You are do not authorized anymore!"
        private const val NOT_AUTHORIZED_YET_MESSAGE = "You are do not authorized!"

        fun buildMainMenu(): ReplyKeyboardMarkup {
            return buildMenu(MainMenu.values().map { it.title })
        }

        fun buildAuthorizeMenu(): ReplyKeyboardMarkup {
            return buildMenu(listOf(AUTHORIZE.title))
        }

        fun buildNotificationMenu(): ReplyKeyboardMarkup {
            return buildMenu(NotificationMenu.values().map { it.title })
        }

        private fun buildMenu(titles: List<String>): ReplyKeyboardMarkup {
            val keyboardMarkup = ReplyKeyboardMarkup().apply {
                resizeKeyboard = true
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

    @Transactional
    fun handleCommand(chatId: Long, text: String): BotApiMethod<out Serializable> {
        val commandType = CommandType.getByName(text) ?: return buildMessage(chatId, COMMAND_NOT_FOUND)

        return when (commandType) {
            CommandType.START, CommandType.HELP -> {
                val user = userService.getUserByChatId(chatId)

                val keyboard = if (user != null) buildMainMenu() else buildAuthorizeMenu()
                buildMessage(chatId, DEFAULT_MESSAGE, keyboard)
            }
            CommandType.AUTHORIZE -> {
                val authUrl = authorizationService.generateAuthUrl(chatId)

                LOG.info("user with chatId=$chatId was authorized/reauthorized")

                buildMessage(chatId, "$SEIZE_TO_AUTHORIZE$authUrl")
            }
            CommandType.REVOKE_AUTH -> {
                if (userService.isAuthorized(chatId)) {
                    userService.revokeAccessToken(chatId)

                    LOG.info("access token was revoked for user with chatId=$chatId")

                    buildMessage(chatId, REVOKE_ACCESS_TOKEN_MESSAGE, buildAuthorizeMenu())
                } else {
                    buildMessage(chatId, NOT_AUTHORIZED_YET_MESSAGE, buildAuthorizeMenu())
                }
            }
        }
    }

    @Transactional
    fun handleOperation(chatId: Long, messageId: Int, text: String): BotApiMethod<out Serializable> {
        if (text == AUTHORIZE.title){
            if (userService.isAuthorized(chatId))
                return buildMessage(chatId, ALREADY_AUTHORIZED, buildMainMenu())

            val authUrl = authorizationService.generateAuthUrl(chatId)
            return buildMessage(chatId, "$SEIZE_TO_AUTHORIZE\n$authUrl", buildAuthorizeMenu())
        }

        if (!userService.isAuthorized(chatId))
            return buildMessage(chatId, INVITATION_TO_AUTHORIZE, buildAuthorizeMenu(), messageId)

        return when (text) {
            STUDIED.title -> {
                return studyOperation.init(chatId)
            }
            NOTIFICATIONS.title -> {
                buildMessage(chatId, OPERATION_DOES_NOT_IMPLEMENT, buildMainMenu())
            }
            ACCOUNT.title -> {
                buildMessage(chatId, OPERATION_DOES_NOT_IMPLEMENT, buildMainMenu())
            }
            MAIN_MENU.title -> {
                buildMessage(chatId, MAIN_MENU_MESSAGE, buildMainMenu())
            }
            else -> {
                // TODO clean code. Remove this call the userService because of copy of BotController.kt.kt:130
                val keyboard = if (userService.isAuthorized(chatId)) buildMainMenu() else buildAuthorizeMenu()

                buildMessage(chatId, MAIN_MENU_MESSAGE, keyboard)
            }
        }
    }

    @Transactional
    fun handleCallback(chatId: Long, messageId: Int, callData: String): BotApiMethod<out Serializable> {
        if (!userService.isAuthorized(chatId))
            return buildMessage(chatId, INVITATION_TO_AUTHORIZE, buildAuthorizeMenu())

        val (operation, step, navigate, value) = callData.split(";")

        val navigateType = NavigateType.valueOf(navigate)

        return when (OperationType.valueOf(operation)) {
            OperationType.STUDIED -> studyOperation.execute(chatId, messageId, step, navigateType, value)
            OperationType.NOTIFICATIONS -> buildMessage(chatId, OPERATION_DOES_NOT_IMPLEMENT, buildNotificationMenu())
        }
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