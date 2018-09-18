package ru.proshik.english.quizlet.telegramBot.controller

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.proshik.english.quizlet.telegramBot.service.UsersService

@RestController
@RequestMapping("api/v1/user")
class UsersController(private val usersService: UsersService) {

    @PostMapping
    fun registration(@RequestParam("chatId") chatId: String) {
        usersService.create(chatId)
    }

}