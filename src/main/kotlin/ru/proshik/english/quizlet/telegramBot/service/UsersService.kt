package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.proshik.english.quizlet.telegramBot.model.User
import ru.proshik.english.quizlet.telegramBot.repository.UsersRepository
import java.time.ZonedDateTime

@Service
class UsersService(private val usersRepository: UsersRepository,
                   private val authenticationService: AuthenticationService) {

    @Transactional
    fun create(chatId: String): String {
        val authUrl = authenticationService.generateAuthUrl(chatId)

        val user = User(ZonedDateTime.now(), chatId)
        usersRepository.save(user)

        return authUrl
    }

    fun userIsExist(chatId: String): Boolean {
        val findByChatId = usersRepository.findByChatId(chatId)

        return findByChatId != null
    }

}