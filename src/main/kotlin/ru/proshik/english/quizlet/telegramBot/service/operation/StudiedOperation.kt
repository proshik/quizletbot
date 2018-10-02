package ru.proshik.english.quizlet.telegramBot.service.operation

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import ru.proshik.english.quizlet.telegramBot.dto.UserGroupsResp
import ru.proshik.english.quizlet.telegramBot.service.MessageBuilder
import ru.proshik.english.quizlet.telegramBot.service.MessageBuilder.Companion.ALL_ITEMS
import ru.proshik.english.quizlet.telegramBot.service.QuizletService
import ru.proshik.english.quizlet.telegramBot.service.operation.StudiedOperation.StepType.GROUP
import ru.proshik.english.quizlet.telegramBot.service.operation.StudiedOperation.StepType.SET
import ru.proshik.english.quizlet.telegramBot.service.vo.ModeType
import ru.proshik.english.quizlet.telegramBot.service.vo.SetStat
import ru.proshik.english.quizlet.telegramBot.service.vo.Studied
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap

@Component
class StudiedOperation(val quizletService: QuizletService) : Operation {

    val messageFormatter = MessageBuilder()

    enum class StepType {
        GROUP,
        SET
    }

    data class ActiveStep(val stepType: StepType, val userGroups: List<UserGroupsResp>, val groupId: Long?)

    data class OperationResult(val studied: Studied)

    private val stepStore = ConcurrentHashMap<Long, ActiveStep>()

    private val operationResultStore = ConcurrentHashMap<Long, OperationResult>()

    override fun init(chatId: Long): InitResult {
        stepStore.remove(chatId)
        operationResultStore.remove(chatId)

        val userGroups = quizletService.userGroups(chatId)

        //TODO do refactoring that ugly code
        val text: String
        val stepType: StepType
        var groupId: Long? = null
        val outputData: List<Pair<String, String>>
        when {
            userGroups.size > 1 -> {
                text = """Select set(s):"""
                stepType = GROUP
                outputData = userGroups.asSequence()
                        .map { group -> Pair(group.name, group.id.toString()) }
                        .toList()
            }
            userGroups.size == 1 -> {
                text = """Select group:"""
                groupId = userGroups[0].id
                stepType = SET
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
            val message = messageFormatter.buildStepPageKeyboardMessage(chatId, text, outputData)
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
                          callData: String): BotApiMethod<out Serializable> {

        // todo finish that
//        val (command, value) = callData.split(";")
//
//        when (command) {
//            MessageBuilder.PAGING_ELEMENT -> {}
//            MessageBuilder.PAGING_BUTTONS -> {}
//        }
        val activeStep = stepStore[chatId] ?: return EditMessageText()
                .setChatId(chatId)
                .setMessageId(messageId)
                .setText("Unexpected transition")
                .setReplyMarkup(null)

        return when (activeStep.stepType) {
            GROUP -> handleSelectGroup(chatId, messageId, callData, activeStep)

            SET -> handleSelectSet(chatId, messageId, callData, activeStep)
        }
    }

    private fun handleSelectGroup(chatId: Long, messageId: Int, callData: String, activeStep: ActiveStep): BotApiMethod<out Serializable> {
        val (command, value) = callData.split(";")

        when (command) {
            MessageBuilder.STEPPING -> {
                val group = activeStep.userGroups.asSequence().filter { group -> group.id.toString() == value }.firstOrNull()

                if (group == null) {
                    stepStore.remove(chatId)
                    return EditMessageText()
                            .setChatId(chatId)
                            .setMessageId(messageId)
                            .setText("Doesn't find group for $value")
                            .setReplyMarkup(null)
                }

                val items = group.sets.asSequence()
//                        .sortedByDescending { set -> set.publishedDate }
                        .map { it -> Pair(it.title, it.id.toString()) }.toList()

                if (items.isEmpty()) {
                    stepStore.remove(chatId)
                    return EditMessageText()
                            .setChatId(chatId)
                            .setMessageId(messageId)
                            .setText("Doesn't find not one set for ${group.name}")
                            .setReplyMarkup(null)
                }

                stepStore[chatId] = ActiveStep(SET, activeStep.userGroups, group.id)

                val text = StringBuilder("Group: *${group.name}*\n")
                text.append("Select a set from group: ")

                val message = messageFormatter.buildStepPageKeyboardMessage(chatId, text.toString(), items, messageId, showAllLine = true)

                // need to remove a previous studied result
                operationResultStore.remove(chatId)

                return message
            }
            else -> throw RuntimeException("unexpected callbackData=$callData")
        }
    }

    private fun handleSelectSet(chatId: Long,
                                messageId: Int,
                                callData: String,
                                activeStep: ActiveStep): BotApiMethod<out Serializable> {
        val (command, value) = callData.split(";")

        return when (command) {
            // paging by result
            MessageBuilder.PAGING_ELEMENT -> {
                val operationResult = operationResultStore[chatId]
                        ?: throw RuntimeException("not available command=\"$command\" for step")

                val countOfItems = operationResult.studied.setsStats.size
                val selectedItem = value.toInt()

                val text = createMessageText(operationResult.studied.groupName, operationResult.studied.setsStats[selectedItem - 1])

                operationResultStore[chatId] = OperationResult(operationResult.studied)

                messageFormatter.buildItemPageKeyboardMessage(chatId, messageId, text, countOfItems, selectedItem)
            }
            // select next step or another varieties of elements
            MessageBuilder.STEPPING -> {
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
                    return EditMessageText()
                            .setChatId(chatId)
                            .setMessageId(messageId)
                            .setText("Incorrect request. The operation will start from the beginning${group.name}")
                            .setReplyMarkup(null)
                }

                val statistics = quizletService.studiedInfo(chatId, group.id, setIds, activeStep.userGroups)

                operationResultStore[chatId] = OperationResult(statistics)

                val text = createMessageText(statistics.groupName, statistics.setsStats[0])

                messageFormatter.buildItemPageKeyboardMessage(chatId, messageId, text, statistics.setsStats.size)
            }
            MessageBuilder.PAGING_BUTTONS -> {
                val iterableStep = stepStore[chatId] ?: return EditMessageText()
                        .setChatId(chatId)
                        .setMessageId(messageId)
                        .setText("Incorrect request. Saved step doesn't find")
                        .setReplyMarkup(null)

                val group = iterableStep.userGroups.asSequence()
                        .filter { group -> group.id == iterableStep.groupId }
                        .first()

                val text = StringBuilder("Group: *${group.name}*\n")
                text.append("Select a set from group: ")

                val items = group.sets.asSequence()
//                        .sortedByDescending { set -> set.publishedDate }
                        .map { it -> Pair(it.title, it.id.toString()) }.toList()

                return messageFormatter.buildStepPageKeyboardMessage(chatId, text.toString(), items, messageId,
                        firstElemInGroup = value.toInt(), pagingButton = true, showAllLine = true)
            }
            else -> throw RuntimeException("unexpected callbackData=$callData")
        }

    }

    fun createMessageText(groupName: String, set: SetStat): String {
        val res = StringBuilder("Group: *$groupName*\n")

        res.append("Set: *${set.title}*\n\n")

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