package ru.proshik.english.quizlet.telegramBot.service.operation

import org.apache.log4j.Logger
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import ru.proshik.english.quizlet.telegramBot.service.AccountService
import ru.proshik.english.quizlet.telegramBot.service.AuthenticationService
import ru.proshik.english.quizlet.telegramBot.service.UsersService
import ru.proshik.english.quizlet.telegramBot.service.operation.BotOperationService.MainMenu.NOTIFICATIONS
import ru.proshik.english.quizlet.telegramBot.service.operation.BotOperationService.MainMenu.STUDIED
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap

@Service
class BotOperationService(private val usersService: UsersService,
                          private val accountService: AccountService,
                          private val authenticationService: AuthenticationService,
                          private val studiedOperation: StudiedOperation,
                          private val botNotificationOperation: BotNotificationOperation) {

    enum class GreetingsMenu(val title: String) {
        AUTHORIZE("Connect to Quizlet.com")
    }

    enum class MainMenu(val title: String) {
        STUDIED("Studied"),
        NOTIFICATIONS("Notifications");
    }

    companion object {

        private val LOG = Logger.getLogger(BotOperationService::class.java)

        val OPERATIONS = MainMenu.values().toList().map { operation -> operation.title }.toList()
    }


    data class ActionOperation(val operation: MainMenu, val messageId: Int? = null)

    private val actionOperationStore = ConcurrentHashMap<Long, ActionOperation>()

    fun handleOperation(chatId: Long, text: String): BotApiMethod<out Serializable> {

        if (GreetingsMenu.AUTHORIZE.title == text) {
            val authorizeUrl = authenticationService.connectToQuizlet(chatId)
            return SendMessage()
                    .setChatId(chatId)
                    .setText(authorizeUrl)
                    .setReplyMarkup(buildAuthorizeMenu())
        }

        val account = accountService.getAccount(chatId)
                ?: return SendMessage()
                        .setChatId(chatId)
                        .setText("Please, authorize with quizlet.com, use the screen keyboard")
                        .setReplyMarkup(buildAuthorizeMenu())



        val mainMenuElement = MainMenu.values().firstOrNull { it.title == text }
                ?: return SendMessage()
                        .setChatId(chatId)
                        .setText("Please, user keyboard")
                        .setReplyMarkup(buildMainMenu())

        return when (mainMenuElement) {
            STUDIED -> {
                val (message, existData) = studiedOperation.init(chatId)

                if (existData)
                    actionOperationStore[chatId] = ActionOperation(STUDIED)
                message
            }
            NOTIFICATIONS -> {
                val (message, existData) = botNotificationOperation.init(chatId)

                if (existData)
                    actionOperationStore[chatId] = ActionOperation(NOTIFICATIONS)
                actionOperationStore[chatId] = ActionOperation(NOTIFICATIONS)
                message
            }
        }
    }

    fun handleCallback(chatId: Long, messageId: Int, callData: String): BotApiMethod<out Serializable> {
        val account = accountService.getAccount(chatId)
                ?: return SendMessage()
                        .setChatId(chatId)
                        .setText("Please, authorize with quizlet.com, use the screen keyboard")
                        .setReplyMarkup(buildAuthorizeMenu())

        val actionOperations = actionOperationStore[chatId]
        return if (actionOperations != null) {
            val (message, _) = when (actionOperations.operation) {
                STUDIED -> studiedOperation.navigate(chatId, messageId, callData)
                NOTIFICATIONS -> studiedOperation.navigate(chatId, messageId, callData)
            }
            message
        } else
            EditMessageReplyMarkup().setChatId(chatId).setMessageId(messageId).setReplyMarkup(null)

    }

    private fun buildAuthorizeMenu(): ReplyKeyboardMarkup {
        val keyboardMarkup = ReplyKeyboardMarkup().apply {
            resizeKeyboard = true
            selective = true
        }

        val rows = ArrayList<KeyboardRow>()
        val row = KeyboardRow()
        row.add(GreetingsMenu.AUTHORIZE.title)
        rows.add(row)

        keyboardMarkup.keyboard = rows

        return keyboardMarkup
    }


    private fun buildMainMenu(): ReplyKeyboardMarkup {
        val keyboardMarkup = ReplyKeyboardMarkup().apply {
            resizeKeyboard = true
            selective = true
            oneTimeKeyboard = true
        }

        val rows = ArrayList<KeyboardRow>()
        for (operation in BotOperationService.OPERATIONS) {
            val row = KeyboardRow()
            row.add(operation)
            rows.add(row)
        }

        keyboardMarkup.keyboard = rows

        return keyboardMarkup
    }

}