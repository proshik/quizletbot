package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import ru.proshik.english.quizlet.telegramBot.client.QuizletClient
import ru.proshik.english.quizlet.telegramBot.dto.SetResp
import ru.proshik.english.quizlet.telegramBot.dto.UserGroupsResp
import ru.proshik.english.quizlet.telegramBot.dto.UserStudiedResp
import ru.proshik.english.quizlet.telegramBot.service.model.*
import ru.proshik.english.quizlet.telegramBot.service.model.ModeType.Companion.designationsByModeTypes

data class StudiedModes(val mode: String,
                        val startDate: Long,
                        val finishDate: Long?,
                        val formattedScore: String?)

data class AccountInfo(val login: String,
                       val accessToken: String)

data class StatFilter(val groupIds: List<Long>?, val setIds: List<Long>?, val modes: List<ModeType>?)

@Service
class QuizletStatisticService(private val quizletClient: QuizletClient) {

    fun userStatistic(statFilter: StatFilter? = null,
                      accountInfo: AccountInfo): MutableMap<UserGroupsResp, Map<SetResp, List<StudiedModes>>> {

        val userGroups = quizletClient.userGroups(accountInfo.login, accountInfo.accessToken)

        val userStudied = quizletClient.userStudied(accountInfo.login, accountInfo.accessToken)

        val studiedSetsBySetId = userStudied
                .asSequence()
                .filter { s -> if (statFilter?.modes != null) designationsByModeTypes(statFilter.modes).contains(s.mode) else true }
                .groupBy { userStudiedResp: UserStudiedResp -> userStudiedResp.set.id }

        val result: MutableMap<UserGroupsResp, Map<SetResp, List<StudiedModes>>> = HashMap()
        for (group in userGroups.filter { statFilter?.groupIds?.contains(it.id) ?: true }) {
            val setStat: MutableMap<SetResp, List<StudiedModes>> = HashMap()
            for (set in group.sets.filter { statFilter?.setIds?.contains(it.id) ?: true }) {
                val studiedModes = studiedSetsBySetId[set.id]
                        ?.filter { us -> us.finishDate != null }
                        .orEmpty()
                        .map { StudiedModes(it.mode, it.startDate, it.finishDate, it.formattedScore) }
                setStat[set] = studiedModes
            }
            result[group] = setStat
        }

        return result
    }

}