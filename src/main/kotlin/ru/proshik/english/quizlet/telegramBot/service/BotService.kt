package ru.proshik.english.quizlet.telegramBot.service

import org.apache.log4j.Logger
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import ru.proshik.english.quizlet.telegramBot.repository.UsersRepository
import ru.proshik.english.quizlet.telegramBot.service.BotService.GreetingsMenu.AUTHORIZE
import ru.proshik.english.quizlet.telegramBot.service.BotService.MainMenu.NOTIFICATIONS
import ru.proshik.english.quizlet.telegramBot.service.BotService.MainMenu.STUDIED
import ru.proshik.english.quizlet.telegramBot.service.BotService.NotificationMenu.*
import ru.proshik.english.quizlet.telegramBot.service.operation.StudiedOperationExecutor
import ru.proshik.english.quizlet.telegramBot.service.vo.CommandType
import ru.proshik.english.quizlet.telegramBot.service.vo.NavigateType
import ru.proshik.english.quizlet.telegramBot.service.vo.OperationType
import java.io.Serializable

@Component
class BotService(private val usersRepository: UsersRepository,
                 private val studiedOperation: StudiedOperationExecutor,
                 private val authenticationService: AuthenticationService) {

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
        //        ANNOUNCEMENT("Announcement new available"),
        MAIN_MENU("Main menu");
    }

    companion object {

        private val LOG = Logger.getLogger(BotService::class.java)

        private const val DEFAULT_MESSAGE = "The bot will help you to get information about studied sets on https://quizlet.com."
        private const val AUTHORIZE_URL = "Please, use the URL to authorize with quizlet's site:\n"
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
                val user = usersRepository.findByChatId(chatId)
                val keyboard = if (user != null) buildMainMenu() else buildAuthorizeMenu()
                buildMessage(chatId, DEFAULT_MESSAGE, keyboard)
            }
            CommandType.AUTHORIZE, CommandType.RE_AUTHORIZE -> {
                val authUrl = authenticationService.generateAuthUrl(chatId)

                LOG.info("user with chatId=$chatId was authorized/reauthorized")

                buildMessage(chatId, "$AUTHORIZE_URL$authUrl")
            }
            CommandType.REVOKE_AUTH -> {
                val user = usersRepository.findByChatId(chatId)
                if (user != null) {
                    usersRepository.deleteByUserId(user.id)

                    LOG.info("access token was revoked for user with chatId=$chatId")

                    buildMessage(chatId, "user ${user.login} was delete", buildAuthorizeMenu())
                } else {
                    buildMessage(chatId, "user doesn't exist", buildAuthorizeMenu())
                }
            }
        }
    }

    @Transactional
    fun handleOperation(chatId: Long, messageId: Int, text: String): BotApiMethod<out Serializable> {
        return when (text) {
            AUTHORIZE.title -> {
                val user = usersRepository.findByChatId(chatId)
                if (user != null) {
                    return buildMessage(chatId, "You've have already authenticated with Quizlet.", buildMainMenu())
                }

                val authUrl = authenticationService.generateAuthUrl(chatId)
                buildMessage(chatId, "$AUTHORIZE_URL $authUrl", buildAuthorizeMenu())
            }
            STUDIED.title -> {
                val user = usersRepository.findByChatId(chatId)
                        ?: return buildMessage(chatId, "Please, authorize with quizlet.com, use the screen keyboard.", buildAuthorizeMenu())



                return studiedOperation.init(chatId)
            }
            NOTIFICATIONS.title -> buildMessage(chatId, "Select a notification type: ", buildNotificationMenu())
            REMINDING.title -> {
                val user = usersRepository.findByChatId(chatId)
                        ?: return buildMessage(chatId, "Please, authorize with quizlet.com, use the screen keyboard.", buildAuthorizeMenu())

//                return notificationOperation.init(chatId, user)
                return SendMessage()
                        .setChatId(chatId)
                        .setText("*Sorry, operation doesn't implement*")
                        .enableMarkdown(true)
            }
            MAIN_MENU.title -> {
                usersRepository.findByChatId(chatId)
                        ?: return buildMessage(chatId, "Please, authorize with quizlet.com, use the screen keyboard.", buildAuthorizeMenu())

                buildMessage(chatId, "Please, use a screen keyboard.", buildMainMenu())
            }
            else -> {
                val keyboard =
                        if (usersRepository.findByChatId(chatId) != null) buildMainMenu()
                        else buildAuthorizeMenu()

                buildMessage(chatId, "Unknown operation. Please, use a keyboard buttons.", keyboard, messageId)
            }
        }
    }

    @Transactional
    fun handleCallback(chatId: Long, messageId: Int, callData: String): BotApiMethod<out Serializable> {
        val user = usersRepository.findByChatId(chatId)
                ?: return buildMessage(chatId, "Please, authorize with quizlet.com, use the screen keyboard.", buildAuthorizeMenu())

        val (operation, step, navigate, value) = callData.split(";")

        val navigateType = NavigateType.valueOf(navigate)

        return when (OperationType.valueOf(operation)) {
            OperationType.STUDIED -> studiedOperation.execute(chatId, messageId, step, navigateType, value)
            OperationType.NOTIFICATIONS -> studiedOperation.execute(chatId, messageId, step, navigateType, value)
//            else -> EditMessageReplyMarkup().setChatId(chatId).setMessageId(messageId).setReplyMarkup(null)
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