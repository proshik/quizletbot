package ru.proshik.english.quizlet.telegramBot

import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TelegramBotApplication

fun main(args: Array<String>) {
    runApplication<TelegramBotApplication>(*args){
        setBannerMode(Banner.Mode.OFF)
    }
}
