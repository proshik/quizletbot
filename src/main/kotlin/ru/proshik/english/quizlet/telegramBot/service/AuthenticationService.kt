package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.proshik.english.quizlet.telegramBot.client.QuizletClient
import ru.proshik.english.quizlet.telegramBot.model.Account
import ru.proshik.english.quizlet.telegramBot.model.User
import java.time.ZonedDateTime

@Service
class AuthenticationService(private val stateService: QuizletStateService,
                            private val quizletClient: QuizletClient,
                            private val usersService: UsersService,
        // TODO change an architecture of classes and remove @Lazy
                            @Lazy private val telegramBot: TelegramBot) {

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
        // TODO fix orElseThrow
        val user = usersService.getUser(chatId) ?: throw RuntimeException("user doesn't find")
        //                .orElseThrow { RuntimeException("user doesn't find") }

        if (user.account != null) {
            // update access token for account
            user.account.accessToken = authorization.accessToken
        } else {
            // create account
            val account = Account(ZonedDateTime.now(), authorization.userId, authorization.accessToken)
            user.account = account
        }
        // save or update an user
        usersService.createUser(user)

        // delete state from im-memory map with states
        stateService.delete(state)

        // get userId(chatId) to notify in the telegram by chatId
        telegramBot.sendAuthConfirmationMessage(user.chatId, user.account.login)
    }

    fun connectToQuizlet(chatId: Long): String {
        val user = usersService.getUser(chatId)
        if (user == null) {
            usersService.createUser(User(ZonedDateTime.now(), chatId))
        }

        return generateAuthUrl(chatId)
    }

    private fun generateAuthUrl(chatId: Long): String {
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
