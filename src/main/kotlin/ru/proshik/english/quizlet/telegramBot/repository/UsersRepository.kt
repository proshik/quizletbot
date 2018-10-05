package ru.proshik.english.quizlet.telegramBot.repository

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import ru.proshik.english.quizlet.telegramBot.model.Users

@Repository
interface UsersRepository : CrudRepository<Users, Long> {

    @Query(value = "select u.id, u.chat_id, u.login, u.access_token from Users u where u.chat_id = :chatId")
    fun findByChatId(@Param("chatId") chatId: Long): Users?

    @Modifying
    @Query(value = "delete from Users where id = :id")
    fun deleteByUserId(@Param("id") id: Long)
}
