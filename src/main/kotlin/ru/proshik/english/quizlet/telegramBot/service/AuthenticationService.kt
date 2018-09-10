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
                            private val usersRepository: UsersRepository,
                            private val telegramService: TelegramService) {

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

    @Transactional
    fun authenticate(state: String, code: String) {
        // check state
        val chatId = stateService.get(state) ?: throw RuntimeException("state value doesn't find by state key=$state")

        // sent request to quizlet
        val authorization = quizletClient.accessToken(code)

        // save access_token to DB
        val user = usersRepository.findByChatId(chatId).orElseThrow { RuntimeException("user doesn't find") }
        // TODO try inside telegram bot. It were write because generated an exception on duplicate login in the account table
//        if (user.account != null) {
//            throw RuntimeException("quizlet account for user with chatId=$chatId already exists")
//        }

        // create and save account for user
        val account = Account(ZonedDateTime.now(), authorization.userId, authorization.accessToken)
        user.account = account
        usersRepository.save(user)

        // delete state from im-memory map with states
        stateService.delete(state)

        // get userId(chatId) to notify in the telegram by chatId
        telegramService.sendAuthConfirmationMessage(user.chatId)
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
