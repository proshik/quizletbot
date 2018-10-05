package ru.proshik.english.quizlet.telegramBot

import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.telegram.telegrambots.ApiContextInitializer

@SpringBootApplication
@EnableJdbcRepositories
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args) {
        // Init Telegram bot API
        ApiContextInitializer.init()
        // disable show banner
        setBannerMode(Banner.Mode.OFF)
    }
}
