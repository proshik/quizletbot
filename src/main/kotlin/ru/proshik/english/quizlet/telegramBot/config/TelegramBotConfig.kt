package ru.proshik.english.quizlet.telegramBot.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.bots.DefaultBotOptions

@Configuration
class TelegramBotConfig(@Value("\${telegram.max-threads}") private val maxThreads: Int) {

    @Bean
    fun defaultBotOptions(): DefaultBotOptions {
        val options = DefaultBotOptions()
        options.maxThreads = maxThreads

        return options
    }

}
