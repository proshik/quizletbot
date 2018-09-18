package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.proshik.english.quizlet.telegramBot.model.User
import ru.proshik.english.quizlet.telegramBot.repository.UsersRepository
import java.time.ZonedDateTime

@Service
class UsersService(private val usersRepository: UsersRepository) {

    @Transactional
    fun create(chatId: String) {
        val user = User(ZonedDateTime.now(), chatId)

        usersRepository.save(user)
    }

    fun getUser(chatId: String): User? {
        return usersRepository.findByChatId(chatId)
    }

}