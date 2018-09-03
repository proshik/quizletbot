package ru.proshik.english.quizlet.telegramBot.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.proshik.english.quizlet.telegramBot.repository.model.User

interface UsersRepository : JpaRepository<User, Long> {
}