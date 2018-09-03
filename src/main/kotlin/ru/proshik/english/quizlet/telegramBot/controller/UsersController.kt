package ru.proshik.english.quizlet.telegramBot.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.proshik.english.quizlet.telegramBot.model.UserResp
import ru.proshik.english.quizlet.telegramBot.service.UsersService

@RestController
@RequestMapping("api/v1/user")
class UsersController(private val usersService: UsersService) {

    @GetMapping
    fun getInfo() : UserResp {
        return usersService.getInfo()
    }


}