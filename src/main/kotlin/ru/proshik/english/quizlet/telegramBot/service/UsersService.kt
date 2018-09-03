package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import ru.proshik.english.quizlet.telegramBot.model.UserResp
import ru.proshik.english.quizlet.telegramBot.repository.UsersRepository

@Service
class UsersService(private val usersRepository: UsersRepository) {

    fun getInfo(): UserResp {
        return UserResp(chatId = "111")
    }

}