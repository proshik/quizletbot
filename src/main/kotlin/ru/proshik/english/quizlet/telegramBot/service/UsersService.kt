package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.proshik.english.quizlet.telegramBot.model.UserResp
import ru.proshik.english.quizlet.telegramBot.repository.UsersRepository
import ru.proshik.english.quizlet.telegramBot.repository.model.User
import java.time.ZonedDateTime

@Service
class UsersService(private val usersRepository: UsersRepository) {

    @Transactional
    fun create(chatId: String) {
        val user = User(createdDate = ZonedDateTime.now(),
                chatId = chatId,
                account = null
        )

        usersRepository.save(user)
    }

    fun getInfo(userId: Long): UserResp {
        return usersRepository.findById(userId)
                .map { mapper -> UserResp(mapper.chatId) }
                .orElse(null)
    }

}