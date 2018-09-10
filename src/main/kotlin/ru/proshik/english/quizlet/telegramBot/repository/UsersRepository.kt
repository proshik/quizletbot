package ru.proshik.english.quizlet.telegramBot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import ru.proshik.english.quizlet.telegramBot.repository.model.User
import java.util.*

@Repository
interface UsersRepository : JpaRepository<User, Long> {

    @Query(value = "select u from User u left join fetch u.account where u.chatId = :chatId")
    fun findByChatId(@Param("chatId") chatId: String): Optional<User>
}