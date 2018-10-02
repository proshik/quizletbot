package ru.proshik.english.quizlet.telegramBot.service

import org.apache.log4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.ActionType
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.updateshandlers.SentCallback
import ru.proshik.english.quizlet.telegramBot.service.TelegramBotHandler.Companion.buildMainMenu
import java.io.Serializable

@Component
class TelegramBot(@Value("\${telegram.token}") private val token: String,
                  @Value("\${telegram.username}") private val username: String,
                  defaultBotOptions: DefaultBotOptions,
                  val telegramBotHandler: TelegramBotHandler) : TelegramLongPollingBot(defaultBotOptions) {

    companion object {

        private val LOG = Logger.getLogger(TelegramBot::class.java)
    }

    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    private fun <T : Serializable> AbsSender.execute(message: BotApiMethod<T>) {
        this.execute(message)
    }

    override fun onUpdateReceived(update: Update) {
        try {
            execute(onWebHookUpdateReceived(update))
        } catch (e: Exception) {
            LOG.error("Panic! Messages not sending! Internal exception.", e)
            sendMessage(buildErrorMessage(update))
        } catch (e: Error) {
            LOG.error("Panic! Messages not sending! Internal error", e)
            sendMessage(buildErrorMessage(update))
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
        update.hasEditedMessage() -> SendMessage().setChatId(update.message.chatId).setText("Operation doesn't support. The edited Message")
        else -> SendMessage().setChatId(update.message.chatId).setText("unexpected operation")
    }

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

    fun sendAuthConfirmationMessage(chatId: Long, login: String) {
        sendMessage(SendMessage()
                .setChatId(chatId)
                .setText("The account was added! ")
                .setReplyMarkup(buildMainMenu()))
    }

    private fun message(update: Update): BotApiMethod<out Serializable> {
        val chatAction = SendChatAction().setChatId(update.message.chatId).setAction(ActionType.TYPING)
        sendMessage(chatAction)

        return when {
            update.message.isCommand -> commandMessage(update)
            //TODO remove or use this type of messages
            update.message.isReply -> SendMessage().setChatId(update.message.chatId).setText("isReply")
            update.message.isUserMessage -> operationMessage(update)
            else -> SendMessage().setChatId(update.message.chatId).setText("unexpected type of message")
        }
    }

    private fun callback(update: Update): BotApiMethod<out Serializable> {
        val chatId = update.callbackQuery.message.chatId
        val messageId = update.callbackQuery.message.messageId
        val callData = update.callbackQuery.data

        val chatAction = SendChatAction().setChatId(chatId).setAction(ActionType.TYPING)
        sendMessage(chatAction)

        return telegramBotHandler.handleCallback(chatId, messageId, callData)
    }

    private fun commandMessage(update: Update): BotApiMethod<out Serializable> {
        return telegramBotHandler.handleCommand(update.message.chatId, update.message.text)
    }

    private fun operationMessage(update: Update): BotApiMethod<out Serializable> {
        return telegramBotHandler.handleOperation(update.message.chatId, update.message.messageId, update.message.text)
    }

    private fun buildErrorMessage(update: Update): SendMessage {
        return SendMessage()
                .setChatId(defineChatId(update))
                .setText("*Oops, there's been a mistake. Please repeat a request*")
                .setParseMode(ParseMode.MARKDOWN)
    }

    private fun defineChatId(update: Update): Long {
        return if (update.hasCallbackQuery()) {
            update.callbackQuery.message.chatId
        } else {
            update.message.chatId
        }
    }

}
