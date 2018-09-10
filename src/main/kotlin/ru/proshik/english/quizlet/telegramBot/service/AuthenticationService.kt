package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.proshik.english.quizlet.telegramBot.client.QuizletClient
import ru.proshik.english.quizlet.telegramBot.repository.UsersRepository
import ru.proshik.english.quizlet.telegramBot.repository.model.Account
import java.time.ZonedDateTime

@Service
class AuthenticationService(private val stateService: QuizletStateService,
                            private val quizletClient: QuizletClient,
                            private val usersRepository: UsersRepository) {

    companion object {
        const val QUIZLET_BASE_URL = "https://quizlet.com/authorize"
    }

    @Value("\${quizlet.client-id}")
    val clientId: String? = null

    @Value("\${quizlet.response-type}")
    val responseType: String? = null

    @Value("\${quizlet.scope}")
    val scope: String? = null

    @Value("\${quizlet.redirect-url}")
    val redirectUrl: String? = null

    @Transactional
    fun authenticate(state: String, code: String) {
        // check state
        val stateValue = stateService.get(state)
                ?: throw RuntimeException("state value doesn't find by state key=$state")

        val user = usersRepository.findById(state.toLong()).orElseThrow { RuntimeException("user doesn't find") }

        // sent request to quizlet
        val authorization = quizletClient.accessToken(code)
        // save access_token to DB

        user.account =
                Account(
                        createdDate = ZonedDateTime.now(),
                        accessToken = authorization.accessToken,
                        login = authorization.userId,
                        user = user
                )
        usersRepository.save(user)

        // get userId(chatId) to notify in the telegram by chatId
    }

    fun generateAuthUrl(): String {
        val state = stateService.add()
        return buildAuthUrl(state)
    }

    fun generateAuthUrl(chatId: String): String {
        val state = stateService.add(chatId)
        return buildAuthUrl(state)
    }

    private fun buildAuthUrl(state: String): String {
        return QUIZLET_BASE_URL +
                "?client_id=$clientId" +
                "&response_type=$responseType" +
                "&state=$state" +
                "&scope=$scope" +
                "&redirect_url=$redirectUrl"
    }

}
