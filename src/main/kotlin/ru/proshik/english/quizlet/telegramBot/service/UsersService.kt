package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.proshik.english.quizlet.telegramBot.model.UserResp
import ru.proshik.english.quizlet.telegramBot.repository.UsersRepository
import ru.proshik.english.quizlet.telegramBot.repository.model.User
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

    fun getInfo(userId: Long): UserResp {
        return usersRepository.findById(userId)
                .map { mapper -> UserResp(mapper.id, mapper.chatId) }
                .orElse(null)
    }

}