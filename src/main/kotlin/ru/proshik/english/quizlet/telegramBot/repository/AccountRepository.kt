package ru.proshik.english.quizlet.telegramBot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.proshik.english.quizlet.telegramBot.repository.model.User

@Repository
interface AccountRepository : JpaRepository<User, Long>