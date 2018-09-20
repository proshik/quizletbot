package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import ru.proshik.english.quizlet.telegramBot.client.QuizletClient
import ru.proshik.english.quizlet.telegramBot.dto.SetResp
import ru.proshik.english.quizlet.telegramBot.dto.UserGroupsResp
import ru.proshik.english.quizlet.telegramBot.dto.UserStudiedResp
import ru.proshik.english.quizlet.telegramBot.model.Account
import ru.proshik.english.quizlet.telegramBot.repository.AccountRepository
import ru.proshik.english.quizlet.telegramBot.service.model.ModeStat
import ru.proshik.english.quizlet.telegramBot.service.model.ModeType
import ru.proshik.english.quizlet.telegramBot.service.model.SetStat
import ru.proshik.english.quizlet.telegramBot.service.model.Statistics

@Service
class QuizletInfoService(private val accountRepository: AccountRepository,
                         private val quizletClient: QuizletClient) {


    fun userGroups(chatId: Long): List<UserGroupsResp> {
        val account = getAccount(chatId)

        return quizletClient.userGroups(account.login, account.accessToken)
    }

    fun buildStatistic(chatId: Long,
                       groupId: Long,
                       setIds: List<Long>,
                       userGroups: List<UserGroupsResp>): Statistics {
        val account = getAccount(chatId)

        val userStudied = quizletClient.userStudied(account.login, account.accessToken)

        val pair = userGroups.asSequence()
                .filter { userGroup -> groupId == userGroup.id }
                .map { userGroup -> Pair(userGroup, userGroup.sets.filter { set -> setIds.contains(set.id) }) }
                .toList()
                .first()

        val setStats = buildStatistic(userStudied, pair.second)

        return Statistics(pair.first.id, pair.first.name, setStats)
    }

    private fun buildStatistic(studied: List<UserStudiedResp>, sets: List<SetResp>): List<SetStat> {
        val studiedSetsBySetId = studied.groupBy { userStudiedResp: UserStudiedResp -> userStudiedResp.set.id }

        return sets.map { set ->
            val modeStats = studiedSetsBySetId[set.id].orEmpty().map { us -> ModeStat(defineModeType(us.mode), us.startDate, us.finishDate, us.formattedScore) }

            SetStat(set.id, set.title, set.url, set.createdDate, set.publishedDate, modeStats)
        }
    }

    private fun defineModeType(mode: String): ModeType {
        return ModeType.modeTypeByDesignation(mode)
    }

    private fun getAccount(chatId: Long): Account {
        return accountRepository.findAccessTokenByUserChatId(chatId.toString())
    }

}