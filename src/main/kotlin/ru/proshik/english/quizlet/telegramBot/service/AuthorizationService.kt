package ru.proshik.english.quizlet.telegramBot.service

import org.apache.log4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.proshik.english.quizlet.telegramBot.client.QuizletClient
import ru.proshik.english.quizlet.telegramBot.exception.AuthorizationException
import ru.proshik.english.quizlet.telegramBot.model.Users
import ru.proshik.english.quizlet.telegramBot.queue.NotificationQueue
import ru.proshik.english.quizlet.telegramBot.queue.Queue
import ru.proshik.english.quizlet.telegramBot.repository.StateRepository
import ru.proshik.english.quizlet.telegramBot.repository.UsersRepository

@Service
class AuthorizationService(private val quizletClient: QuizletClient,
                           private val usersRepository: UsersRepository,
                           private val stateRepository: StateRepository,
                           private val notificationQueue: NotificationQueue) {

    companion object {
        private val LOG = Logger.getLogger(AuthorizationService::class.java)

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
    fun authorization(state: String, code: String) {
        // check state
        val chatId = stateRepository.get(state)
                ?: throw AuthorizationException("state value doesn't find by state key=$state")
        // sent request to quizlet
        val authorization = quizletClient.accessToken(code)
        // save access_token to DB
        var user = usersRepository.findByChatId(chatId)
        if (user == null) {
            // create account
            user = Users(chatId, authorization.userId, authorization.accessToken)
        } else {
            // update an user login and access token
            user.login = authorization.userId
            user.accessToken = authorization.accessToken
        }
        usersRepository.save(user)
        // delete state from im-memory map with states
        stateRepository.delete(state)
        // push conformation message to queue for notify an user telegram bot
        val text = "You are successfully authorized as ${user.login}!"
        notificationQueue.put(Queue.Message(chatId, text))

        LOG.info("success authorize account=${authorization.userId} user=${user.id}")
    }

    fun generateAuthUrl(chatId: Long): String {
        val state = stateRepository.add(chatId)
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
