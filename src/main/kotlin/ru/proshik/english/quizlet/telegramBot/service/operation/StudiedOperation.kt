package ru.proshik.english.quizlet.telegramBot.service.operation

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import ru.proshik.english.quizlet.telegramBot.dto.UserGroupsResp
import ru.proshik.english.quizlet.telegramBot.service.*
import ru.proshik.english.quizlet.telegramBot.service.MessageFormatter.Companion.ALL_ITEMS
import ru.proshik.english.quizlet.telegramBot.service.model.ModeType
import ru.proshik.english.quizlet.telegramBot.service.model.SetStat
import ru.proshik.english.quizlet.telegramBot.service.model.Studied
import ru.proshik.english.quizlet.telegramBot.service.operation.StudiedOperation.StepType.SELECT_GROUP
import ru.proshik.english.quizlet.telegramBot.service.operation.StudiedOperation.StepType.SELECT_SET
import java.util.concurrent.ConcurrentHashMap

@Component
class StudiedOperation(val quizletInfoService: QuizletInfoService) : Operation {

    val messageFormatter = MessageFormatter()

    enum class StepType {
        SELECT_GROUP,
        SELECT_SET
    }

    data class ActiveStep(val stepType: StepType, val userGroups: List<UserGroupsResp>, val groupId: Long?)

    data class OperationResult(val studied: Studied)

    private val stepStore = ConcurrentHashMap<Long, ActiveStep>()

    private val operationResultStore = ConcurrentHashMap<Long, OperationResult>()

    override fun init(chatId: Long): InitResult {
        stepStore.remove(chatId)
        operationResultStore.remove(chatId)

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
//                        .sortedByDescending { set -> set.publishedDate }
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
            InitResult(SendMessage()
                    .setChatId(chatId)
                    .setText("User classes doesn't find"), false)
        }
    }

    override fun navigate(chatId: Long,
                          messageId: Int,
                          callData: String): StepResult {

        // todo finish that
//        val (command, value) = callData.split(";")
//
//        when (command) {
//            MessageFormatter.PAGING_ELEMENT -> {}
//            MessageFormatter.PAGING_BUTTONS -> {}
//        }
        val activeStep = stepStore[chatId] ?: return StepResult(EditMessageText()
                .setChatId(chatId)
                .setMessageId(messageId)
                .setText("Unexpected transition")
                .setReplyMarkup(null), true)

        return when (activeStep.stepType) {
            SELECT_GROUP -> handleSelectGroup(chatId, messageId, callData, activeStep)

            SELECT_SET -> handleSelectSet(chatId, messageId, callData, activeStep)
        }

//        val operationResult = operationResultStore[chatId]
    }

    private fun handleSelectGroup(chatId: Long, messageId: Int, callData: String, activeStep: ActiveStep): StepResult {
        val (command, value) = callData.split(";")

        when (command) {
            MessageFormatter.STEPPING -> {
                val group = activeStep.userGroups.asSequence().filter { group -> group.id.toString() == value }.firstOrNull()

                if (group == null) {
                    stepStore.remove(chatId)
                    return StepResult(EditMessageText()
                            .setChatId(chatId)
                            .setMessageId(messageId)
                            .setText("Doesn't find group for $value")
                            .setReplyMarkup(null), true)
                }

                val items = group.sets.asSequence()
//                        .sortedByDescending { set -> set.publishedDate }
                        .map { it -> Pair(it.title, it.id.toString()) }.toList()

                if (items.isEmpty()) {
                    stepStore.remove(chatId)
                    return StepResult(EditMessageText()
                            .setChatId(chatId)
                            .setMessageId(messageId)
                            .setText("Doesn't find not one set for ${group.name}")
                            .setReplyMarkup(null), true)

                }

                stepStore[chatId] = ActiveStep(SELECT_SET, activeStep.userGroups, group.id)

                val text = StringBuilder("Group: *${group.name}*\n")
                text.append("Select a set from group: ")

                val message = messageFormatter.navigateBySteps(chatId, text.toString(), items, messageId, showAllLine = true)

                // need to remove a previous studied result
                operationResultStore.remove(chatId)

                return StepResult(message, false)
            }
            else -> throw RuntimeException("unexpected callbackData=$callData")
        }
    }

    private fun handleSelectSet(chatId: Long, messageId: Int, callData: String, activeStep: ActiveStep): StepResult {
        val (command, value) = callData.split(";")

        return when (command) {
            // paging by result
            MessageFormatter.PAGING_ELEMENT -> {
                val operationResult = operationResultStore[chatId]
                        ?: throw RuntimeException("not available command=\"$command\" for step")


                val countOfItems = operationResult.studied.setsStats.size
                val selectedItem = value.toInt()

                val text = createMessageText(operationResult.studied.groupName, operationResult.studied.setsStats[selectedItem - 1])

                val message = messageFormatter.navigateByItems(chatId, messageId, text, countOfItems, selectedItem)

                operationResultStore[chatId] = OperationResult(operationResult.studied)

                StepResult(message, true)
            }
            // select next step or another varieties of elements
            MessageFormatter.STEPPING -> {
                if (operationResultStore[chatId] != null) {
                    operationResultStore.remove(chatId)
                }

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

                if (setIds.isEmpty()) {
                    val message = EditMessageText()
                            .setChatId(chatId)
                            .setMessageId(messageId)
                            .setText("Incorrect request. The operation will start from the beginning${group.name}")
                            .setReplyMarkup(null)

                    return StepResult(message, true)
                }

                val statistics = quizletInfoService.buildStatistic(chatId, group.id, setIds, activeStep.userGroups)

                operationResultStore[chatId] = OperationResult(statistics)

                val text = createMessageText(statistics.groupName, statistics.setsStats[0])

                val message = messageFormatter.navigateByItems(chatId, messageId, text, statistics.setsStats.size)

                StepResult(message, true)
            }
            MessageFormatter.PAGING_BUTTONS -> {
                val iterableStep = stepStore[chatId]
                if (iterableStep == null) {
                    val message = EditMessageText()
                            .setChatId(chatId)
                            .setMessageId(messageId)
                            .setText("Incorrect request. Saved step doesn't find")
                            .setReplyMarkup(null)

                    return StepResult(message, true)
                }

                val group = iterableStep.userGroups.asSequence()
                        .filter { group -> group.id == iterableStep.groupId }
                        .first()

//                stepStore[chatId] = ActiveStep(SELECT_SET, activeStep.userGroups, group.id)

                val text = StringBuilder("Group: *${group.name}*\n")
                text.append("Select a set from group: ")

                val items = group.sets.asSequence()
//                        .sortedByDescending { set -> set.publishedDate }
                        .map { it -> Pair(it.title, it.id.toString()) }.toList()

                val message = messageFormatter.navigateBySteps(chatId, text.toString(), items, messageId,
                        firstElemInGr = value.toInt(), pagingButton = true, showAllLine = true)

                return StepResult(message, true)
            }
            else -> throw RuntimeException("unexpected callbackData=$callData")
        }

    }

    fun createMessageText(groupName: String, set: SetStat): String {
        val res = StringBuilder("Group: *$groupName*\n\n")

        res.append("Set: *${set.title}*\n")

        val modeStatByMode = set.modeStats.groupBy { modeStat -> modeStat.mode }.toMap()

        for (mode in ModeType.values()) {
            if (modeStatByMode.containsKey(mode)) {
                val modeStat = modeStatByMode[mode]
                if (modeStat == null || modeStat.isEmpty()) {
                    res.append("${mode.title} (*-*)\n")
                    continue
                }

                val lastModeStat = modeStat.asSequence().filter { it.finishDate != null }.sortedByDescending { it.finishDate }.firstOrNull()

                val valueStat = if (lastModeStat?.finishDate != null) "*finished* [${modeStat.size}]" else "started"
                res.append("_${mode.title}_ ($valueStat) ")

                // TODO handle it beautifully
                if (lastModeStat != null) {
                    if (lastModeStat.formattedScore != null) res.append(" ${lastModeStat.formattedScore}")
                }

                res.append("\n")
            } else {
                res.append("${mode.title} (*-*)\n")
            }
        }
        res.append(set.url)
        res.append("\n")

        return res.toString()
    }


}