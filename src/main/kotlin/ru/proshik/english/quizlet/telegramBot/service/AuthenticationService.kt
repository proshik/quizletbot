package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import ru.proshik.english.quizlet.telegramBot.client.QuizletClient

@Service
class AuthenticationService(private val stateService: QuizletStateService,
                            private val quizletClient: QuizletClient) {

    companion object {
        const val QUIZLET_BASE_URL = "https://quizlet.com/authorize"
    }

    @Value("\${quizlet.client-id}")
    private val clientId: String? = null

    @Value("\${quizlet.response-type}")
    private val responseType: String? = null

    @Value("\${quizlet.scope}")
    private val scope: String? = null

    @Value("\${quizlet.redirect-url}")
    private val redirectUrl: String? = null

    fun authenticate(state: String, code: String) {
        // check state
        if (stateService.checkState(state)) {

        }

        // sent request to quizlet
        val authorization = quizletClient.getAccessToken()
        // save access_token to DB

        // get userId(chatId) to notify in the telegram by chatId
    }

    fun buildAuthUrl(): String {
        val state = stateService.generateState()

        return QUIZLET_BASE_URL +
                "?client_id=$clientId" +
                "&response_type=$responseType" +
                "&state=$state" +
                "&scope=$scope" +
                "&redirect_url=$redirectUrl"
    }

}
