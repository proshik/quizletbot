package ru.proshik.english.quizlet.telegramBot.controller

import org.apache.log4j.Logger
import org.springframework.web.bind.annotation.*
import ru.proshik.english.quizlet.telegramBot.exception.AuthenticationException
import ru.proshik.english.quizlet.telegramBot.service.AuthenticationService
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping("redirect")
class QuizletController(private val authenticationService: AuthenticationService) {

    companion object {

        private val LOG = Logger.getLogger(QuizletController::class.java)
    }

    @GetMapping
    fun authRedirect(@RequestParam("state") state: String,
                     @RequestParam("code") code: String,
                     httpServletResponse: HttpServletResponse) {

        authenticationService.authenticate(state, code)

        httpServletResponse.setHeader("Location", "/")
        httpServletResponse.status = 302
    }

    @ExceptionHandler
    fun handleRuntimeException(ex: AuthenticationException) {
        LOG.warn("${ex.message}")
    }

    @ExceptionHandler
    fun handleRuntimeException(ex: Exception) {
        LOG.error("unexpected authentication exception", ex)
    }

}
