package ru.proshik.english.quizlet.telegramBot.service

import org.apache.log4j.Logger
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.updateshandlers.SentCallback
import java.io.Serializable

@Service
class TelegramBotService(@Lazy private val bot: TelegramBot) {

    companion object {

        private val LOG = Logger.getLogger(TelegramBotService::class.java)
    }

    fun onWebHookUpdateReceived(update: Update): BotApiMethod<Message> {
        return SendMessage()
                .setChatId(update.message?.chatId)
                .setText("Hello " + update.message.text + "!")
    }

    fun <T : Serializable> sendMessage(message: BotApiMethod<T>) {
        try {
            bot.execute(message)
        } catch (e: TelegramApiException) {
            LOG.error("send message", e)
        }

    }

    fun <T : Serializable> sendMessage(message: BotApiMethod<T>, callback: SentCallback<T>) {
        try {
            bot.executeAsync(message, callback)
        } catch (e: TelegramApiException) {
            LOG.error("async send message")
        }

    }

    fun sendAuthConfirmationMessage(chatId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
