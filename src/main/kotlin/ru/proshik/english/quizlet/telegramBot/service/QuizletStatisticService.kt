package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import ru.proshik.english.quizlet.telegramBot.client.QuizletClient
import ru.proshik.english.quizlet.telegramBot.dto.SetResp
import ru.proshik.english.quizlet.telegramBot.dto.UserGroupsResp
import ru.proshik.english.quizlet.telegramBot.dto.UserStudiedResp
import ru.proshik.english.quizlet.telegramBot.service.model.AccountInfo
import ru.proshik.english.quizlet.telegramBot.service.model.ModeType
import ru.proshik.english.quizlet.telegramBot.service.model.StatInputParam
import ru.proshik.english.quizlet.telegramBot.service.model.StudiedModes

@Service
class QuizletStatisticService(private val quizletClient: QuizletClient) {

    fun userStatistic(statInputParam: StatInputParam = StatInputParam(),
                      accountInfo: AccountInfo): MutableMap<UserGroupsResp, Map<SetResp, List<StudiedModes>>> {

        val userGroups = quizletClient.userGroups(accountInfo.login, accountInfo.accessToken)

        val userStudied = quizletClient.userStudied(accountInfo.login, accountInfo.accessToken)

        val studiedSetsBySetId = userStudied
                .filter { u -> if (statInputParam.modes.isNotEmpty()) ModeType.designationsByModeTypes(statInputParam.modes).contains(u.mode) else true }
                .groupBy { userStudiedResp: UserStudiedResp -> userStudiedResp.set.id }

        val groupStat: MutableMap<UserGroupsResp, Map<SetResp, List<StudiedModes>>> = HashMap()
        for (group in userGroups.filter { group -> if (statInputParam.groupIds.isNotEmpty()) statInputParam.groupIds.contains(group.id) else true }) {

            val setStat: MutableMap<SetResp, List<StudiedModes>> = HashMap()
            for (set in group.sets.filter { set -> if (statInputParam.setIds.isNotEmpty()) statInputParam.setIds.contains(set.id) else true }) {
                val studiedModes = studiedSetsBySetId[set.id]
                        ?.filter { us -> us.finishDate != null }
                        .orEmpty()
                        .map { us -> StudiedModes(us.mode, us.startDate, us.finishDate, us.formattedScore) }
                setStat[set] = studiedModes
            }
            groupStat[group] = setStat
        }

        return groupStat
    }
}
