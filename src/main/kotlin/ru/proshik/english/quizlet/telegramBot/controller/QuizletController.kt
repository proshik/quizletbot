package ru.proshik.english.quizlet.telegramBot.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.proshik.english.quizlet.telegramBot.service.AuthenticationService

@RestController
@RequestMapping("redirect")
class QuizletController(private val authenticationService: AuthenticationService) {

    @GetMapping
    fun authRedirect(@RequestParam("state") state: String,
                     @RequestParam("code") code: String) {
        authenticationService.authenticate(state, code)
    }

}
