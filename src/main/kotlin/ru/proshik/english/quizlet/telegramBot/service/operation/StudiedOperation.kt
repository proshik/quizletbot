package ru.proshik.english.quizlet.telegramBot.service.operation

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import ru.proshik.english.quizlet.telegramBot.service.QuizletInfoService
import ru.proshik.english.quizlet.telegramBot.service.action.MessageFormatter
import java.io.Serializable

@Component
class StudiedOperation(val quizletInfoService: QuizletInfoService) : Operation {

    val messageFormatter = MessageFormatter()

    enum class StudiedOperationStep : OperationStep {
        SELECT_GROUP,
        SELECT_SET;
    }

    override fun initOperation(chatId: Long): OperationStepInfo? {
        val userGroups = quizletInfoService.userGroups(chatId)

        return when {
            userGroups.size > 1 -> {
                val outputData = userGroups.asSequence()
                        .map { group -> Pair(group.name, group.id.toString()) }
                        .toList()

                val operationInfo = OperationPipeline(OperationType.NOTIFICATIONS, StudiedOperationStep.SELECT_GROUP)

                OperationStepInfo(operationInfo, outputData)
            }
            userGroups.size == 1 -> {
                val outputData = userGroups[0].sets.asSequence()
                        .sortedByDescending { set -> set.publishedDate }
                        .map { set -> Pair(set.title, set.id.toString()) }
                        .toList()


                val data = mapOf("group_id" to userGroups[0].id.toString())
                val operationInfo = OperationPipeline(OperationType.NOTIFICATIONS, StudiedOperationStep.SELECT_SET, data)

                OperationStepInfo(operationInfo, outputData)
            }
            userGroups.isEmpty() -> null
            else -> throw RuntimeException("unreacheble path for initialize studied opeartion")
        }
    }

    override fun nextSubOperation(chatId: Long, messageId: Int, callData: String, operationPipeline: OperationPipeline): BotApiMethod<out Serializable> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}