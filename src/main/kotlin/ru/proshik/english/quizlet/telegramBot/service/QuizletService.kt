package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import ru.proshik.english.quizlet.telegramBot.client.QuizletClient
import ru.proshik.english.quizlet.telegramBot.dto.SetResp
import ru.proshik.english.quizlet.telegramBot.dto.UserGroupsResp
import ru.proshik.english.quizlet.telegramBot.dto.UserStudiedResp
import ru.proshik.english.quizlet.telegramBot.model.Account
import ru.proshik.english.quizlet.telegramBot.repository.AccountRepository
import ru.proshik.english.quizlet.telegramBot.service.vo.*

@Service
class QuizletService(private val quizletClient: QuizletClient,
                     private val accountRepository: AccountRepository) {

    fun userGroups(chatId: Long, accountContext: AccountContext): List<UserGroupsResp> {
        return quizletClient.userGroups(accountContext.login, accountContext.accessToken)
    }

    fun studiedInfo(chatId: Long,
                    groupId: Long,
                    setIds: List<Long>,
                    userGroups: List<UserGroupsResp>,
                    accountContext: AccountContext): Studied {
        val userStudied = quizletClient.userStudied(accountContext.login, accountContext.accessToken)

        val pair = userGroups.asSequence()
                .filter { groupId == it.id }
                .map { userGroup -> Pair(userGroup, userGroup.sets.filter { setIds.contains(it.id) }) }
                .toList()
                .first()

        val setStats = studiedInfo(userStudied, pair.second)

        return Studied(pair.first.id, pair.first.name, setStats)
    }

    private fun studiedInfo(studied: List<UserStudiedResp>, sets: List<SetResp>): List<SetStat> {
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
        return accountRepository.findAccountByUserChatId(chatId) ?: throw RuntimeException("unexpected behaviour")
    }

}