package ru.proshik.english.quizlet.telegramBot.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.proshik.english.quizlet.telegramBot.service.AuthenticationService
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping("redirect")
class QuizletController(private val authenticationService: AuthenticationService) {

    @GetMapping
    fun authRedirect(@RequestParam("state") state: String,
                     @RequestParam("code") code: String,
                     httpServletResponse: HttpServletResponse) {

        authenticationService.authenticate(state, code)

        // TODO change location to telegram url if state from telegram bot or to / if from web
        httpServletResponse.setHeader("Location", "/")
        httpServletResponse.status = 302
    }

}
