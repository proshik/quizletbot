package ru.proshik.english.quizlet.telegramBot.controller

import org.springframework.web.bind.annotation.*
import ru.proshik.english.quizlet.telegramBot.dto.UserResp
import ru.proshik.english.quizlet.telegramBot.service.UsersService

@RestController
@RequestMapping("api/v1/user")
class UsersController(private val usersService: UsersService) {

    @PostMapping
    fun registration(@RequestParam("chatId") chatId: String): String {
        return usersService.create(chatId)
    }

}