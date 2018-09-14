package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import ru.proshik.english.quizlet.telegramBot.client.QuizletClient
import ru.proshik.english.quizlet.telegramBot.dto.SetResp
import ru.proshik.english.quizlet.telegramBot.dto.UserGroupsResp
import ru.proshik.english.quizlet.telegramBot.dto.UserStudiedResp
import ru.proshik.english.quizlet.telegramBot.model.Account
import ru.proshik.english.quizlet.telegramBot.repository.AccountRepository
import ru.proshik.english.quizlet.telegramBot.service.model.ModeStat
import ru.proshik.english.quizlet.telegramBot.service.model.SetStat
import ru.proshik.english.quizlet.telegramBot.service.model.Statistics

@Service
class QuizletInfoService(private val accountRepository: AccountRepository,
                         private val quizletClient: QuizletClient) {


    fun userGroups(chatId: String): List<UserGroupsResp> {
        val account = getAccount(chatId)

        return quizletClient.userGroups(account.login, account.accessToken)
    }

    fun buildStatistic(chatId: String,
                       groupId: Long,
                       setIds: List<Long>,
                       userGroups: List<UserGroupsResp>): Statistics {
        val account = getAccount(chatId)

        val userStudied = quizletClient.userStudied(account.login, account.accessToken)

        val group = userGroups.asSequence()
                .filter { userGroup -> groupId == userGroup.id }
                .map { userGroup -> Pair(userGroup, userGroup.sets.filter { set -> setIds.contains(set.id) }) }
                .first()
                .first

        val setStats = buildStatistic(userStudied, group.sets)

        return Statistics(group.id, group.name, setStats)
    }

    private fun buildStatistic(studied: List<UserStudiedResp>, sets: List<SetResp>): List<SetStat> {

        val studiedSetsBySetId = studied.groupBy { userStudiedResp: UserStudiedResp -> userStudiedResp.set.id }

        // TODO put a breakpoint inside the map function
        return sets.map { set ->
            val modeStats = studiedSetsBySetId[set.id]
                    ?.filter { us -> us.finishDate != null }
                    .orEmpty()
                    .map { us -> ModeStat(us.mode, us.startDate, us.finishDate, us.formattedScore) }

            SetStat(set.id, set.title, set.createdDate, set.publishedDate, modeStats)
        }
    }

    private fun getAccount(chatId: String): Account {
        return accountRepository.findAccessTokenByUserChatId(chatId)
    }

}