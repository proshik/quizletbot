package ru.proshik.english.quizlet.telegramBot.service.operation

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import ru.proshik.english.quizlet.telegramBot.dto.UserGroupsResp
import ru.proshik.english.quizlet.telegramBot.service.QuizletInfoService
import ru.proshik.english.quizlet.telegramBot.service.action.MessageFormatter
import ru.proshik.english.quizlet.telegramBot.service.action.MessageFormatter.Companion.ALL_ITEMS
import ru.proshik.english.quizlet.telegramBot.service.model.ModeType
import ru.proshik.english.quizlet.telegramBot.service.model.Statistics
import ru.proshik.english.quizlet.telegramBot.service.operation.StudiedOperation.StepType.SELECT_GROUP
import ru.proshik.english.quizlet.telegramBot.service.operation.StudiedOperation.StepType.SELECT_SET
import java.util.concurrent.ConcurrentHashMap

@Component
class StudiedOperation(val quizletInfoService: QuizletInfoService) : Operation {

    val messageFormatter = MessageFormatter()

    companion object {
        const val GROUP_ID = "group_id"
    }

    enum class StepType {
        SELECT_GROUP,
        SELECT_SET
    }

    data class ActiveStep(val stepType: StepType, val userGroups: List<UserGroupsResp>, val groupId: Long?)

    data class OperationResult(val showedItem: Int, val statistics: Statistics)

    private val stepStore = ConcurrentHashMap<Long, ActiveStep>()

    private val operationResultStore = ConcurrentHashMap<Long, OperationResult>()

    override fun init(chatId: Long): InitResult {
        val userGroups = quizletInfoService.userGroups(chatId)

        //TODO do refactoring that ugly code
        val text: String
        val stepType: StepType
        var groupId: Long? = null
        val outputData: List<Pair<String, String>>
        when {
            userGroups.size > 1 -> {
                text = """Select set(s):"""
                stepType = SELECT_GROUP
                outputData = userGroups.asSequence()
                        .map { group -> Pair(group.name, group.id.toString()) }
                        .toList()
            }
            userGroups.size == 1 -> {
                text = """Select group:"""
                groupId = userGroups[0].id
                stepType = SELECT_SET
                outputData = userGroups[0].sets.asSequence()
                        .sortedByDescending { set -> set.publishedDate }
                        .map { set -> Pair(set.title, set.id.toString()) }
                        .toList()
            }
            else -> throw RuntimeException("unreacheble path for initialize studied operation")
        }

        return if (outputData.isNotEmpty()) {
            // save information about active step
            stepStore[chatId] = ActiveStep(stepType, userGroups, groupId)
            // build text message with keyboard
            val message = messageFormatter.navigateBySteps(chatId, text, outputData)
            // result object
            InitResult(message, true)
        } else {
            InitResult(SendMessage().setChatId(chatId).setText("User classes doesn't find"), false)
        }
    }

    override fun navigate(chatId: Long,
                          messageId: Int,
                          callData: String): StepResult {

        if (callData == null){

        }

        val activeStep = stepStore[chatId]
                ?: return StepResult(SendMessage().setChatId(chatId).setText("unexpected transition"), true)

        return when (activeStep.stepType) {
            SELECT_GROUP -> {
                buildSetsMessage(chatId, messageId, callData, activeStep)
            }
            SELECT_SET -> buildStudied(chatId, messageId, callData, activeStep)
        }

//        val operationResult = operationResultStore[chatId]
    }

    private fun buildSetsMessage(chatId: Long, messageId: Int, callData: String, activeStep: ActiveStep): StepResult {
        val (command, value) = callData.split(";")

        when (command) {
            MessageFormatter.NAVIGATION -> TODO()
            MessageFormatter.ELEMENT -> {
                val group = activeStep.userGroups.asSequence().filter { group -> group.id.toString() == value }.firstOrNull()

                if (group == null) {
                    stepStore.remove(chatId)
                    return StepResult(SendMessage().setChatId(chatId).setText("Doesn't find roup for $value"), true)
//                            .setReplyMarkup(buildDefaultKeyboard())
                }

                val items = group.sets.asSequence()
                        .sortedByDescending { set -> set.publishedDate }
                        .map { it -> Pair(it.title, it.id.toString()) }.toList()

                if (items.isEmpty()) {
                    stepStore.remove(chatId)
                    return StepResult(SendMessage().setChatId(chatId).setText("Doesn't find not one set for ${group.name}"), true)
//                            .setReplyMarkup(buildDefaultKeyboard())
                }

                stepStore[chatId] = ActiveStep(SELECT_SET, activeStep.userGroups, group.id)

                val text = StringBuilder("Group: *${group.name}*\n")
                text.append("Select a set from group: ")

                val message = messageFormatter.navigateBySteps(chatId, text.toString(), items, messageId, showAllLine = true)

                // need to remove a previous statustics result
                operationResultStore.remove(chatId)

                return StepResult(message, false)
            }
            else -> throw RuntimeException("unexpected callbackData=$callData")
        }
    }

    private fun buildStudied(chatId: Long, messageId: Int, callData: String, activeStep: ActiveStep): StepResult {
        val (command, value) = callData.split(";")

//        val operationResult = operationResultStore[chatId]
//
//        // if operation to that message is finished then execute to navigate
//        if (operationResult != null) {
//
//        }

        return when (command) {
            MessageFormatter.NAVIGATION -> {
                val operationResult = operationResultStore[chatId]
                        ?: throw RuntimeException("not available command=$command for step")

                val text = createMessageText(operationResult.statistics)

                val countOfItems = operationResult.statistics.setsStats.size
                val selectedItem = value.toInt()
                val message = messageFormatter.navigateByItems(chatId, messageId, text, countOfItems, selectedItem)

                operationResultStore[chatId] = OperationResult(value.toInt(), operationResult.statistics)

                StepResult(message, true)
            }
            MessageFormatter.ELEMENT -> {
                val group = activeStep.userGroups.asSequence()
                        .filter { group -> group.id == activeStep.groupId }
                        .first()

                val setIds = if (value == ALL_ITEMS) {
                    group.sets.map { set -> set.id }
                } else {
                    group.sets.asSequence()
                            .filter { set -> set.id.toString() == value }
                            .map { set -> set.id }
                            .toList()
                }

                stepStore.remove(chatId)

                if (setIds.isEmpty()) {
                    val message = SendMessage()
                            .setChatId(chatId)
                            .setText("Incorrect request. The operation will start from the beginning${group.name}")
//                            .setReplyMarkup(buildDefaultKeyboard())

                    return StepResult(message, true)
                }

                val statistics = quizletInfoService.buildStatistic(chatId, group.id, setIds, activeStep.userGroups)

                operationResultStore[chatId] = OperationResult(1, statistics)

                val text = createMessageText(statistics)

                val message = messageFormatter.navigateByItems(chatId, messageId, text, statistics.setsStats.size)

                StepResult(message, true)
            }
            else -> throw RuntimeException("unexpected callbackData=$callData")
        }

    }

    fun createMessageText(statistics: Statistics): String {
        val res = StringBuilder("Group: *${statistics.groupName}*\n\n")

        val sets = statistics.setsStats.asSequence()
                .sortedByDescending { set -> set.publishedDate }

        val set = sets.first()
//        for (set in sets.fir) {
        res.append("Set: *${set.title}*\n")

        val modeStatByMode = set.modeStats.groupBy { modeStat -> modeStat.mode }.toMap()

        for (mode in ModeType.values()) {
            if (modeStatByMode.containsKey(mode)) {
                val modeStat = modeStatByMode[mode]

                val valueStat = if (modeStat?.last()?.finishDate != null) "*finished* [${modeStat.size}]" else "started"
                res.append("_${mode.title}_ ($valueStat) ")

                // TODO handle it beautifully
                //                    if (modeStat?.formattedScore != null) res.append(" ${modeStat.formattedScore}")

                res.append("\n")
            } else {
                res.append("${mode.title} (*-*)\n")
            }
        }
//            res.append(set.url)
        res.append("\n")
//        }
        return res.toString()
    }


}