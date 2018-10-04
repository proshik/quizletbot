package ru.proshik.english.quizlet.telegramBot.service.operation

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import ru.proshik.english.quizlet.telegramBot.dto.UserGroupsResp
import ru.proshik.english.quizlet.telegramBot.model.Account
import ru.proshik.english.quizlet.telegramBot.repository.AccountRepository
import ru.proshik.english.quizlet.telegramBot.service.BotService
import ru.proshik.english.quizlet.telegramBot.service.MessageBuilder
import ru.proshik.english.quizlet.telegramBot.service.MessageBuilder.Companion.ALL_ITEMS
import ru.proshik.english.quizlet.telegramBot.service.QuizletService
import ru.proshik.english.quizlet.telegramBot.service.operation.StudiedOperationService.StepType.*
import ru.proshik.english.quizlet.telegramBot.service.vo.*
import ru.proshik.english.quizlet.telegramBot.service.vo.NavigateType.*
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap

@Component
class StudiedOperationService(val quizletService: QuizletService,
                              val accountRepository: AccountRepository,
                              private val objectMapper: ObjectMapper) : Operation {

    val messageFormatter = MessageBuilder()

    enum class StepType {
        GROUP,
        SET,
        RESULT
    }

    data class StudiedOperationInfo(val stepType: StepType,
                                    val userGroups: List<UserGroupsResp>,
                                    val groupId: Long? = null,
                                    val studied: Studied? = null)

    data class ActiveStep(val stepType: StepType, val userGroups: List<UserGroupsResp>, val groupId: Long?)

    data class OperationResult(val studied: Studied)

    private val stepStore = ConcurrentHashMap<Long, ActiveStep>()

    private val operationResultStore = ConcurrentHashMap<Long, OperationResult>()

    private val accountContext: (account: Account) -> AccountContext = { AccountContext(it.login, it.accessToken) }

    override fun init(chatId: Long, account: Account): BotApiMethod<out Serializable> {
        stepStore.remove(chatId)
        operationResultStore.remove(chatId)

        val userGroups = quizletService.userGroups(chatId, accountContext(account))

        //TODO do refactoring that ugly code
        val text: String
        val stepType: StepType
        var groupId: Long? = null
        val outputData: List<Pair<String, String>>
        when {
            userGroups.size > 1 -> {
                text = "*Please, select a set(s):*\n"
                stepType = GROUP
                outputData = userGroups.asSequence()
                        .map { group -> Pair(group.name, group.id.toString()) }
                        .toList()
            }
            userGroups.size == 1 -> {
                text = "*Please, select a class:*\n"
                groupId = userGroups[0].id
                stepType = SET
                outputData = userGroups[0].sets.asSequence()
                        .map { set -> Pair(set.title, set.id.toString()) }
                        .toList()
            }
            else -> throw RuntimeException("unreacheble path for initialize studied operation")
        }

        return if (outputData.isNotEmpty()) {
            // save information about active step
            stepStore[chatId] = ActiveStep(stepType, userGroups, groupId)
            // save info into db about active operation
            val value = objectMapper.writeValueAsString(StudiedOperationInfo(stepType, userGroups))
            account.operationData = objectMapper.writeValueAsString(OperationData(BotService.MainMenu.STUDIED.title, value))
            accountRepository.save(account)

            // build text message with keyboard
            messageFormatter.buildStepPageKeyboardMessage(chatId, text, outputData)
        } else {
            SendMessage().setChatId(chatId).setText("User classes doesn't find")
        }
    }

    override fun execute(chatId: Long,
                         messageId: Int,
                         callData: String,
                         operationData: OperationData,
                         account: Account): BotApiMethod<out Serializable> {

        val (navigateTypeName, data) = callData.split(";")

        val navigateType = NavigateType.values().firstOrNull { it.name == navigateTypeName }
                ?: throw RuntimeException("unexpected navigateType=$navigateTypeName")

        val activeStep = stepStore[chatId] ?: return buildCleanEditMessage(chatId, messageId)

//        operationData
//
//        when (navigateType) {
//
//            PAGING_BY_ITEM -> TODO()
//            PAGING_BY_BUTTON -> TODO()
//            NEXT_STEP -> TODO()
//        }

        return when (activeStep.stepType) {
            GROUP -> handleSelectGroup(chatId, messageId, callData, navigateType, activeStep)

            SET -> handleSelectSet(chatId, messageId, callData, navigateType, activeStep, account)

            RESULT -> handleSelectResult(chatId, messageId, callData, navigateType)
        }
    }

    fun handleSelectGroup(chatId: Long, messageId: Int, callData: String, navigateType: NavigateType, activeStep: ActiveStep): BotApiMethod<out Serializable> {
        val (command, value) = callData.split(";")

        when (navigateType) {
            NEXT_STEP -> {
                val group = activeStep.userGroups.asSequence()
                        .filter { it.id.toString() == value }
                        .firstOrNull()
                if (group == null) {
                    stepStore.remove(chatId)
                    return buildCleanEditMessage(chatId, messageId)
                }

                val items = group.sets.asSequence()
                        .map { it -> Pair(it.title, it.id.toString()) }.toList()

                if (items.isEmpty()) {
                    stepStore.remove(chatId)
                    return EditMessageText()
                            .setChatId(chatId)
                            .setMessageId(messageId)
                            .setText("Class *${group.name}* doesn't contain any sets.")
                            .enableMarkdown(true)
                            .setReplyMarkup(null)
                }

                stepStore[chatId] = ActiveStep(SET, activeStep.userGroups, group.id)

                val text = StringBuilder("Class: *${group.name}*\n\n")
                text.append("*Please, select a set from class:*")

                val message = messageFormatter.buildStepPageKeyboardMessage(chatId, text.toString(), items, messageId, showAllLine = true)

                // need to remove a previous studied result
                operationResultStore.remove(chatId)

                return message
            }
            PAGING_BY_BUTTON -> TODO() // need to implement when groups (classes) will be include more than 4 items
            else -> return buildCleanKeyboardMessage(chatId, messageId)
        }
    }

    private fun handleSelectSet(chatId: Long,
                                messageId: Int,
                                callData: String,
                                navigateType: NavigateType,
                                activeStep: ActiveStep,
                                account: Account): BotApiMethod<out Serializable> {
        val (command, value) = callData.split(";")

        return when (navigateType) {
            // select next step or another varieties of elements
            NEXT_STEP -> {
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

                if (setIds.isEmpty()) return buildCleanEditMessage(chatId, messageId)

                val statistics = quizletService.studiedInfo(chatId, group.id, setIds, activeStep.userGroups, accountContext(account))

                stepStore[chatId] = ActiveStep(RESULT, activeStep.userGroups, group.id)

                operationResultStore[chatId] = OperationResult(statistics)

                // build text message for first element
                val text = createMessageText(statistics.groupName, statistics.setsStats[0])

                messageFormatter.buildItemPageKeyboardMessage(chatId, messageId, text, statistics.setsStats.size)
            }
            PAGING_BY_BUTTON -> {
                val iterableStep = stepStore[chatId] ?: return buildCleanEditMessage(chatId, messageId)

                val group = iterableStep.userGroups.asSequence()
                        .filter { group -> group.id == iterableStep.groupId }
                        .first()

                val text = StringBuilder("Class: *${group.name}*\n")
                text.append("Please, select a set from the class *${group.name}:*\n")

                val items = group.sets.asSequence()
                        .map { it -> Pair(it.title, it.id.toString()) }
                        .toList()

                return messageFormatter.buildStepPageKeyboardMessage(chatId, text.toString(), items, messageId,
                        firstElemInGroup = value.toInt(), pagingButton = true, showAllLine = true)
            }
            else -> return buildCleanKeyboardMessage(chatId, messageId)
        }
    }

    private fun handleSelectResult(chatId: Long, messageId: Int, callData: String, navigateType: NavigateType): BotApiMethod<out Serializable> {
        val (command, value) = callData.split(";")
        // paging by result
        return when (navigateType) {
            PAGING_BY_ITEM -> {
                val operationResult = operationResultStore[chatId]
                        ?: throw RuntimeException("not available command=\"$command\" for step")

                val countOfItems = operationResult.studied.setsStats.size
                val selectedItem = value.toInt()

                val text = createMessageText(operationResult.studied.groupName, operationResult.studied.setsStats[selectedItem - 1])

                operationResultStore[chatId] = OperationResult(operationResult.studied)

                messageFormatter.buildItemPageKeyboardMessage(chatId, messageId, text, countOfItems, selectedItem)
            }
            else -> return buildCleanKeyboardMessage(chatId, messageId)
        }
    }

    private fun buildCleanEditMessage(chatId: Long, messageId: Int): BotApiMethod<out Serializable> {
        return EditMessageText()
                .setChatId(chatId)
                .setMessageId(messageId)
                .enableMarkdown(true)
                .setText("*Please, continue the further new operation.*")
//                .setReplyMarkup(null)
    }

    private fun buildCleanKeyboardMessage(chatId: Long, messageId: Int): BotApiMethod<out Serializable> {
        return EditMessageReplyMarkup()
                .setChatId(chatId)
                .setMessageId(messageId)
                .setReplyMarkup(null)
    }

    private fun createMessageText(groupName: String, set: SetStat): String {
        val res = StringBuilder("Class: *$groupName*\n")

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

                // TODO build the text it beautifully, as on the quizlet.com in the section of "your study sets"
                if (lastModeStat != null) {
                    if (lastModeStat.formattedScore != null) res.append(" ${lastModeStat.formattedScore}")
                }

                res.append("\n")
            } else {
                res.append("${mode.title} (*-*)\n")
            }
        }
        res.append("\nURL: ${set.url}")

        return res.toString()
    }


}