package ru.proshik.english.quizlet.telegramBot.service

import org.apache.log4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.updateshandlers.SentCallback
import java.io.Serializable
import java.util.*

@Component
class TelegramBotService(@Value("\${telegram.token}") private val token: String,
                         @Value("\${telegram.username}") private val username: String,
                         defaultBotOptions: DefaultBotOptions,
                         val quizletOperationService: QuizletOperationService) : TelegramLongPollingBot(defaultBotOptions) {

    companion object {

        private val LOG = Logger.getLogger(TelegramBotService::class.java)

        private const val DEFAULT_MESSAGE = "This bot can help you get information about studied sets on quizlet.com"
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

    fun onWebHookUpdateReceived(update: Update): BotApiMethod<Message> = when {
        update.hasMessage() -> message(update)
        update.hasCallbackQuery() -> callback(update)
        else -> throw RuntimeException("Unexpected situation")
    }

    private fun message(update: Update): BotApiMethod<Message> = when {
        update.message.isCommand -> commandMessage(update)
        else -> operationMessage(update)
    }

    private fun callback(update: Update): BotApiMethod<Message> {
        val chatId = update.callbackQuery.message.chatId!!
        val messageId = update.callbackQuery.message.messageId!!.toLong()
        val callData = update.callbackQuery.data

        return quizletOperationService.handleCallback(chatId, messageId, callData)
//        return SendMessage().setChatId(update.callbackQuery.message.chatId).setText("Callback message")
    }

    private fun commandMessage(update: Update): BotApiMethod<Message> {
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

    private fun operationMessage(update: Update): BotApiMethod<Message> {
        val chatId = update.message.chatId
        val text = update.message.text

        return quizletOperationService.handleCommand(chatId, text)
    }


    private fun keyboard(update: Update): BotApiMethod<Message> {
        val message = SendMessage() // Create a message object object
                .setChatId(update.message.chatId)
                .setText("You send /start")

        val markupInline = InlineKeyboardMarkup()

        val rowsInline = ArrayList<List<InlineKeyboardButton>>()

        val rowInline = ArrayList<InlineKeyboardButton>()
        rowInline.add(InlineKeyboardButton().setText("Update message text").setCallbackData("update_msg_text"))

        // Set the keyboard to the markup
        rowsInline.add(rowInline)

        // Add it to the message
        markupInline.keyboard = rowsInline
        message.replyMarkup = markupInline

        return message
    }

//    fun sendCustomKeyboard(update: Update): BotApiMethod<Message> {
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
