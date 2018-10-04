package ru.proshik.english.quizlet.telegramBot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import ru.proshik.english.quizlet.telegramBot.model.User

@Repository
interface UsersRepository : JpaRepository<User, Long> {

    @Query(value = "select u from User u where u.chatId = :chatId")
    fun findByChatId(@Param("chatId") chatId: Long): User?

    @Modifying
    @Query(value = "delete from User where id = :id")
    fun deleteByUserId(@Param("id") id: Long)
}