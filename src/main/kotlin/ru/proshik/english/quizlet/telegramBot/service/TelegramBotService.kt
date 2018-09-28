package ru.proshik.english.quizlet.telegramBot.service

import org.apache.log4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.ActionType
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.bots.AbsSender
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.updateshandlers.SentCallback
import ru.proshik.english.quizlet.telegramBot.service.operation.BotOperationService
import java.io.Serializable

@Component
class TelegramBotService(@Value("\${telegram.token}") private val token: String,
                         @Value("\${telegram.username}") private val username: String,
                         defaultBotOptions: DefaultBotOptions,
                         val botOperationService: BotOperationService,
                         val botCommandService: BotCommandService) : TelegramLongPollingBot(defaultBotOptions) {

    companion object {

        private val LOG = Logger.getLogger(TelegramBotService::class.java)
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
            LOG.error("Panic! Messages not sending! Internal exception.", e)
        } catch (e: NotImplementedError) {
            LOG.error("not implement exception")
        } catch (e: Error) {
            LOG.error("Panic! Messages not sending! Internal error", e)
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
        update.hasEditedMessage() -> SendMessage().setChatId(update.message.chatId).setText("Operaotion doesn't support. The edited Message")
        else -> SendMessage().setChatId(update.message.chatId).setText("unexpected operation")
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

        return botOperationService.handleCallback(chatId, messageId, callData)
    }

    private fun commandMessage(update: Update): BotApiMethod<out Serializable> {
        return botCommandService.handleCommand(update.message.chatId, update.message.text)
    }

    private fun operationMessage(update: Update): BotApiMethod<out Serializable> {
        return botOperationService.handleOperation(update.message.chatId, update.message.text)
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
                .setReplyMarkup(buildMainMenu())
                .setText("Account quizlet.com for user $login added"))
    }

    private fun buildMainMenu(): ReplyKeyboardMarkup {
        val keyboardMarkup = ReplyKeyboardMarkup().apply {
            resizeKeyboard = true
            selective = true
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
