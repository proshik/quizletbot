package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import ru.proshik.english.quizlet.telegramBot.model.Users
import ru.proshik.english.quizlet.telegramBot.repository.UsersRepository

@Service
class UserService(val usersRepository: UsersRepository) {

    fun getUserByChatId(chatId: Long): Users? {
        return usersRepository.findByChatId(chatId)
    }

    fun isAuthorized(chatId: Long): Boolean {
        val user = usersRepository.findByChatId(chatId) ?: return false

        return user.accessToken != null
    }

    fun revokeAccessToken(chatId: Long) {
        val user = usersRepository.findByChatId(chatId)
        if (user != null) {
            usersRepository.revokeAccessToken(chatId)
        }
    }


}