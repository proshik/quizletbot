package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.proshik.english.quizlet.telegramBot.model.User
import ru.proshik.english.quizlet.telegramBot.repository.UsersRepository

@Service
class UsersService(private val usersRepository: UsersRepository) {

    fun getUser(chatId: Long): User? {
        return usersRepository.findByChatId(chatId)
    }

    @Transactional
    fun createUser(user: User) {
        usersRepository.save(user)
    }

}