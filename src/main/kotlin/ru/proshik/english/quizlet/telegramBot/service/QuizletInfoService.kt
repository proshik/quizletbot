package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import ru.proshik.english.quizlet.telegramBot.client.QuizletClient
import ru.proshik.english.quizlet.telegramBot.dto.SetResp
import ru.proshik.english.quizlet.telegramBot.dto.UserGroupsResp
import ru.proshik.english.quizlet.telegramBot.dto.UserStudiedResp
import ru.proshik.english.quizlet.telegramBot.model.Account
import ru.proshik.english.quizlet.telegramBot.service.model.ModeStat
import ru.proshik.english.quizlet.telegramBot.service.model.ModeType
import ru.proshik.english.quizlet.telegramBot.service.model.SetStat
import ru.proshik.english.quizlet.telegramBot.service.model.Studied

@Service
class QuizletInfoService(private val accountService: AccountService,
                         private val quizletClient: QuizletClient) {

    // TODO throw access token to that method signature
    fun userGroups(chatId: Long): List<UserGroupsResp> {
        val account = getAccount(chatId)

        return quizletClient.userGroups(account.login, account.accessToken)
    }

    fun buildStatistic(chatId: Long,
                       groupId: Long,
                       setIds: List<Long>,
                       userGroups: List<UserGroupsResp>): Studied {
        val account = getAccount(chatId)

        val userStudied = quizletClient.userStudied(account.login, account.accessToken)

        val pair = userGroups.asSequence()
                .filter { groupId == it.id }
                .map { userGroup -> Pair(userGroup, userGroup.sets.filter { setIds.contains(it.id) }) }
                .toList()
                .first()

        val setStats = buildStatistic(userStudied, pair.second)

        return Studied(pair.first.id, pair.first.name, setStats)
    }

    private fun buildStatistic(studied: List<UserStudiedResp>, sets: List<SetResp>): List<SetStat> {
        val studiedSetsBySetId = studied.groupBy { userStudiedResp: UserStudiedResp -> userStudiedResp.set.id }

        return sets.map { set ->
            val modeStats = studiedSetsBySetId[set.id]
                    .orEmpty()
                    .map { ModeStat(defineModeType(it.mode), it.startDate, it.finishDate, it.formattedScore) }

            SetStat(set.id, set.title, set.url, set.createdDate, set.publishedDate, modeStats)
        }
    }

    private fun defineModeType(mode: String): ModeType {
        return ModeType.modeTypeByDesignation(mode)
    }

    private fun getAccount(chatId: Long): Account {
        return accountService.getAccount(chatId) ?: throw RuntimeException("unexpected behaviour")
    }

}