package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import ru.proshik.english.quizlet.telegramBot.client.QuizletClient
import ru.proshik.english.quizlet.telegramBot.dto.SetResp
import ru.proshik.english.quizlet.telegramBot.dto.UserGroupsResp
import ru.proshik.english.quizlet.telegramBot.dto.UserStudiedResp
import ru.proshik.english.quizlet.telegramBot.service.model.*
import ru.proshik.english.quizlet.telegramBot.service.model.ModeType.Companion.designationsByModeTypes

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
        for (group in userGroups.filter { group -> statFilter?.groupIds?.contains(group.id) ?: true }) {
            val setStat: MutableMap<SetResp, List<StudiedModes>> = HashMap()
            for (set in group.sets.filter { set -> statFilter?.setIds?.contains(set.id) ?: true }) {
                val studiedModes = studiedSetsBySetId[set.id]
                        ?.filter { us -> us.finishDate != null }
                        .orEmpty()
                        .map { us -> StudiedModes(us.mode, us.startDate, us.finishDate, us.formattedScore) }
                setStat[set] = studiedModes
            }
            result[group] = setStat
        }

        return result
    }

}