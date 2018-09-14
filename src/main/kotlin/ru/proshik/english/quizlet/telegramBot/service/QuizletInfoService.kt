package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import ru.proshik.english.quizlet.telegramBot.client.QuizletClient
import ru.proshik.english.quizlet.telegramBot.dto.UserGroupsResp
import ru.proshik.english.quizlet.telegramBot.service.model.AccountInfo

@Service
class QuizletInfoService(private val quizletClient: QuizletClient) {

    fun userGroups(accountInfo: AccountInfo): List<UserGroupsResp> {
        return quizletClient.userGroups(accountInfo.login, accountInfo.accessToken)
    }

}