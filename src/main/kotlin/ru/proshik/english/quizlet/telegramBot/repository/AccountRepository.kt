package ru.proshik.english.quizlet.telegramBot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import ru.proshik.english.quizlet.telegramBot.model.Account

@Repository
interface AccountRepository : JpaRepository<Account, Long> {

    @Query(value = "select a from Account a join fetch a.user u where u.chatId = :chatId")
    fun findAccountByUserChatId(@Param("chatId") chatId: Long): Account?

    @Modifying
    @Query(value = "delete from Account where id = :id")
    fun deleteByAccountId(@Param("id") id: Long)

}
