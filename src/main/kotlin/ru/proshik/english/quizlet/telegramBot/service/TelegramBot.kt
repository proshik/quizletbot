package ru.proshik.english.quizlet.telegramBot.service

import org.apache.log4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

@Component
class TelegramBot(private val telegramBotService: TelegramBotService,
                  @Value("\${telegram.token}") private val token: String,
                  @Value("\${telegram.login}") private val username: String) : TelegramLongPollingBot() {

    companion object {
        private val LOG = Logger.getLogger(TelegramBot::class.java)
    }

    override fun onUpdateReceived(update: Update) {
        val response = telegramBotService.onWebHookUpdateReceived(update)
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

}
