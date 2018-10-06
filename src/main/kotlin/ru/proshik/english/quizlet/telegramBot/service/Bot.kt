package ru.proshik.english.quizlet.telegramBot.service

import org.apache.log4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.meta.updateshandlers.SentCallback
import ru.proshik.english.quizlet.telegramBot.queue.NotificationQueue
import java.io.Serializable
import javax.annotation.PostConstruct

@Component
class Bot(@Value("\${telegram.token}") private val token: String,
          @Value("\${telegram.username}") private val username: String,
          private val botService: BotService,
          private val notificationQueue: NotificationQueue,
          defaultBotOptions: DefaultBotOptions) : TelegramLongPollingBot(defaultBotOptions) {

    companion object {

        private val LOG = Logger.getLogger(Bot::class.java)
    }

    @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
    private fun <T : Serializable> AbsSender.execute(message: BotApiMethod<T>) {
        this.execute(message)
    }

    @PostConstruct
    fun init() {
        registerNotificationScheduler()
    }

    override fun onUpdateReceived(update: Update) {
        try {
            execute(onWebHookUpdateReceived(update))
        } catch (e: TelegramApiRequestException) {
            LOG.warn("send message with message: ${e.message} and apiResponse: ${e.apiResponse}")
        } catch (e: Exception) {
            LOG.error("message doesn't send", e)
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

    fun onWebHookUpdateReceived(update: Update): BotApiMethod<out Serializable> {
        return when {
            update.hasMessage() -> message(update)
            update.hasCallbackQuery() -> callback(update)
            //TODO investigate this behaviour
            update.hasEditedMessage() -> SendMessage()
                    .setChatId(update.message.chatId)
                    .setText("OperationData doesn't support, edited message")
            else -> SendMessage().setChatId(update.message.chatId).setText("unexpected operation")
        }
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

    private fun registerNotificationScheduler() {
        Thread {
            while (true) {
                val notifyMessage = try {
                    notificationQueue.take()
                } catch (ex: Exception) {
                    LOG.error("read from queue", ex)
                    continue
                }

                val message = SendMessage()
                        .setChatId(notifyMessage.chatId)
                        .setText(notifyMessage.text)
                        .setReplyMarkup(BotService.buildMainMenu())

                try {
                    execute(message)
                } catch (e: TelegramApiException) {
                    LOG.error("notification messages doesn't send", e)
                }
            }
        }.start()
    }

    private fun message(update: Update): BotApiMethod<out Serializable> {
//        sendMessage(SendChatAction().setChatId(update.message.chatId).setAction(ActionType.TYPING))

        val chatId = update.message.chatId
        val text = update.message.text

        return when {
            update.message.isCommand -> botService.handleCommand(chatId, text)
            //TODO investigate this behaviour
            update.message.isReply -> SendMessage()
                    .setChatId(chatId)
                    .setText("OperationData doesn't support, isReply message")
            update.message.isUserMessage -> botService.handleOperation(chatId, update.message.messageId, text)
            else -> SendMessage()
                    .setChatId(chatId)
                    .setText("unexpected type of message")
        }
    }

    private fun callback(update: Update): BotApiMethod<out Serializable> {
        val chatId = update.callbackQuery.message.chatId
        val messageId = update.callbackQuery.message.messageId
        val callData = update.callbackQuery.data

//        sendMessage(SendChatAction().setChatId(chatId).setAction(ActionType.TYPING))

        return botService.handleCallback(chatId, messageId, callData)
    }


    private fun buildErrorMessage(update: Update): SendMessage {
        return SendMessage()
                .setChatId(defineChatId(update))
                .setText("*Oops, there's been a mistake. Please, connect with a developer and try to repeat a request*")
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
