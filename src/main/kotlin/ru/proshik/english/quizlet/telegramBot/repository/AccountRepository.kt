package ru.proshik.english.quizlet.telegramBot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.proshik.english.quizlet.telegramBot.repository.model.Accounts

@Repository
interface AccountRepository : JpaRepository<Accounts, Long>
