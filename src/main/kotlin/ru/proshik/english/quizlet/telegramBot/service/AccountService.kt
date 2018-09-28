package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import ru.proshik.english.quizlet.telegramBot.model.Account
import ru.proshik.english.quizlet.telegramBot.repository.AccountRepository

@Service
class AccountService(private val accountRepository: AccountRepository) {

    fun getAccount(chatId: Long): Account? {
        return accountRepository.findAccountByUserChatId(chatId)
    }

}