package ru.proshik.english.quizlet.telegramBot.repository

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import ru.proshik.english.quizlet.telegramBot.model.Users
import ru.proshik.english.quizlet.telegramBot.repository.mapper.UserCredentialsMapper
import ru.proshik.english.quizlet.telegramBot.service.vo.UserCredentials

@Repository
interface UsersRepository : CrudRepository<Users, Long> {

    @Query(value = "select u.id, u.chat_id, u.login, u.access_token from Users u where u.chat_id = :chatId")
    fun findByChatId(@Param("chatId") chatId: Long): Users?

    @Query(value = "select u.login, u.access_token from Users u where u.chat_id = :chatId", rowMapperClass = UserCredentialsMapper::class)
    fun getUserCredentials(@Param("chatId") chatId: Long): UserCredentials

    @Modifying
    @Query(value = "delete from Users where id = :id")
    fun deleteByUserId(@Param("id") id: Long)

    @Query(value = "select u.access_token is  from Users u where u.chat_id = :chatId")
    fun checkUserAuthentication(@Param("chatId") chatId: Long)

    @Modifying
    @Query(value = "update users set access_token = null where u.chat_id = :chatId")
    fun removeUserAccessToken(@Param("chatId") chatId: Long)
}
