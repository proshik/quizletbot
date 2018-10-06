package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import ru.proshik.english.quizlet.telegramBot.client.QuizletClient
import ru.proshik.english.quizlet.telegramBot.dto.SetResp
import ru.proshik.english.quizlet.telegramBot.dto.UserGroupsResp
import ru.proshik.english.quizlet.telegramBot.dto.UserStudiedResp
import ru.proshik.english.quizlet.telegramBot.model.Users
import ru.proshik.english.quizlet.telegramBot.repository.UsersRepository
import ru.proshik.english.quizlet.telegramBot.service.vo.*

@Service
class QuizletService(private val quizletClient: QuizletClient,
                     private val usersRepository: UsersRepository) {

    fun userGroups(chatId: Long): List<UserGroupsResp> {
        val userCredentials = usersRepository.getUserCredentials(chatId)

        return quizletClient.userGroups(userCredentials.login, userCredentials.accessToken)
    }

    fun studiedInfo(chatId: Long,
                    groupId: Long,
                    setIds: List<Long>,
                    userGroups: List<UserGroupsResp>): Studied {
        val user = getUser(chatId)

        val userStudied = quizletClient.userStudied(user.login, user.accessToken)

        val pair = userGroups.asSequence()
                .filter { groupId == it.id }
                .map { userGroup -> Pair(userGroup, userGroup.sets.filter { setIds.contains(it.id) }) }
                .toList()
                .first()

        val setStats = buildSetStats(userStudied, pair.second)

        return Studied(StudiedClass(pair.first.id, pair.first.name, pair.first.url), setStats)
    }

    private fun buildSetStats(studied: List<UserStudiedResp>, sets: List<SetResp>): List<StudiedSet> {
        val studiedSetsBySetId = studied.groupBy { userStudiedResp: UserStudiedResp -> userStudiedResp.set.id }

        return sets.map { set ->
            val studiedModes = studiedSetsBySetId[set.id]
                    .orEmpty()
                    .map { StudiedMode(defineModeType(it.mode), it.startDate, it.finishDate, it.formattedScore) }

            StudiedSet(set.id, set.title, set.url, set.createdDate, set.publishedDate, studiedModes)
        }
    }

    private fun defineModeType(mode: String): ModeType {
        return ModeType.modeTypeByDesignation(mode)
    }

    private fun getUser(chatId: Long): Users {
        return usersRepository.findByChatId(chatId) ?: throw RuntimeException("unexpected behaviour")
    }

}