package ru.proshik.english.quizlet.telegramBot.service

import org.apache.log4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.updateshandlers.SentCallback
import ru.proshik.english.quizlet.telegramBot.service.operation.OperationService
import java.io.Serializable

@Component
class TelegramBotService(@Value("\${telegram.token}") private val token: String,
                         @Value("\${telegram.username}") private val username: String,
                         defaultBotOptions: DefaultBotOptions,
                         val quizletOperationService: QuizletOperationService,
                         val operationService: OperationService) : TelegramLongPollingBot(defaultBotOptions) {

    companion object {

        private val LOG = Logger.getLogger(TelegramBotService::class.java)

        private const val DEFAULT_MESSAGE = "This bot can help you get information about studied sets on quizlet.com"
    }

    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    private fun <T : Serializable> AbsSender.execute(message: BotApiMethod<T>) {
        this.execute(message)
    }

    override fun onUpdateReceived(update: Update) {
        val response = onWebHookUpdateReceived(update)
        try {
            execute(response)
        } catch (e: TelegramApiException) {
            LOG.error("Panic! Messages not sending!", e)
        }
    }

    override fun getBotUsername(): String {
        return username
    }

    override fun getBotToken(): String {
        return token
    }

    fun onWebHookUpdateReceived(update: Update): BotApiMethod<out Serializable> = when {
        update.hasMessage() -> message(update)
        update.hasCallbackQuery() -> callback(update)
        update.hasEditedMessage() -> SendMessage().setChatId(update.message.chatId).setText("doesn't support operation. The edited Message")
        else -> SendMessage().setChatId(update.message.chatId).setText("unexpected operation")
    }

    private fun message(update: Update): BotApiMethod<out Serializable> = when {
        update.message.isCommand -> commandMessage(update)
        update.message.isReply -> SendMessage().setChatId(update.message.chatId).setText("isReply")
        update.message.isUserMessage -> operationMessage(update)
        else -> SendMessage().setChatId(update.message.chatId).setText("unexpected type of message")
    }

    private fun callback(update: Update): BotApiMethod<out Serializable> {
        val chatId = update.callbackQuery.message.chatId!!
        val messageId = update.callbackQuery.message.messageId
        val callData = update.callbackQuery.data

        return operationService.handleCallback(chatId, messageId, callData)
    }

    private fun commandMessage(update: Update): BotApiMethod<out Serializable> {
        return when (update.message.text.split(" ")[0]) {
            "/start" -> SendMessage().setChatId(update.message?.chatId).setText(DEFAULT_MESSAGE)
            "/help" -> SendMessage().setChatId(update.message?.chatId).setText(DEFAULT_MESSAGE)
            "/connect" -> {
                val chatId = update.message.chatId
                val quizletConnectUrl = quizletOperationService.connectToQuizlet(chatId)

                SendMessage().setChatId(update.message?.chatId).setText(quizletConnectUrl)
            }
            else -> throw RuntimeException("Unexpected command")
        }
    }

    private fun operationMessage(update: Update): BotApiMethod<out Serializable> {
        return operationService.handleOperation(update.message.chatId, update.message.text)
    }


//    fun sendCustomKeyboard(update: Update): BotApiMethod<out Serializable> {
//        val message = SendMessage()
//        message.chatId = update.message.chatId.toString()
//        message.text = "To send text messages, please use the keyboard provided or the commands /start and /help."
//
//        // Create ReplyKeyboardMarkup object
//        val keyboardMarkup = ReplyKeyboardMarkup()
//
//        // Create the keyboard (list of keyboard rows)
//        val rows = ArrayList<KeyboardRow>()
//
//        // Set each button, you can also use KeyboardButton objects if you need something else than text
//        val statistics = KeyboardRow()
//        statistics.add("Statistics")
//
//        val notifications = KeyboardRow()
//        notifications.add("Notifications")
//
//        val account = KeyboardRow()
//        account.add("Account")
//
//        rows.add(statistics)
//        rows.add(notifications)
//        rows.add(account)
//
//        // Set the keyboard to the markup
//        keyboardMarkup.keyboard = rows
//        // Add it to the message
//        message.replyMarkup = keyboardMarkup
//
//        return message
//    }

    fun <T : Serializable> sendMessage(message: BotApiMethod<T>) {
        try {
            execute(message)
        } catch (e: TelegramApiException) {
            LOG.error("send message", e)
        }

    }

    fun <T : Serializable> sendMessage(message: BotApiMethod<T>, callback: SentCallback<T>) {
        try {
            executeAsync(message, callback)
        } catch (e: TelegramApiException) {
            LOG.error("async send message")
        }

    }

    fun sendAuthConfirmationMessage(chatId: String, login: String) {
        sendMessage(SendMessage()
                .setChatId(chatId)
                .setText("Account quizlet.com for user $login added"))
    }

}
