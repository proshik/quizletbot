package ru.proshik.english.quizlet.telegramBot.controller

import org.springframework.web.bind.annotation.*
import ru.proshik.english.quizlet.telegramBot.model.UserResp
import ru.proshik.english.quizlet.telegramBot.service.UsersService

@RestController
@RequestMapping("api/v1/user")
class UsersController(private val usersService: UsersService) {

    @PostMapping
    fun registration(@RequestParam("chatId") chatId: String): String {
        return usersService.create(chatId)
    }

    @GetMapping
    fun getInfo(@RequestParam("userId") userId: Long): UserResp {
        return usersService.getInfo(userId = userId)
    }

}