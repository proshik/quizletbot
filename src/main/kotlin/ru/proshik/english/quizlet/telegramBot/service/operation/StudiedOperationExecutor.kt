package ru.proshik.english.quizlet.telegramBot.service.operation

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import ru.proshik.english.quizlet.telegramBot.dto.UserGroupsResp
import ru.proshik.english.quizlet.telegramBot.model.User
import ru.proshik.english.quizlet.telegramBot.service.MessageBuilder
import ru.proshik.english.quizlet.telegramBot.service.MessageBuilder.Companion.ALL_ITEMS
import ru.proshik.english.quizlet.telegramBot.service.QuizletService
import ru.proshik.english.quizlet.telegramBot.service.operation.StudiedOperationExecutor.StepType.*
import ru.proshik.english.quizlet.telegramBot.service.vo.*
import ru.proshik.english.quizlet.telegramBot.service.vo.NavigateType.*
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap

@Component
class StudiedOperationExecutor(val quizletService: QuizletService,
                               private val objectMapper: ObjectMapper) : OperationExecutor {

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

    private val userContext: (user: User) -> UserContext = { UserContext(it.login, it.accessToken) }


    // Notes:
    // - callback format: <operationType>:<navigateType>:<itemId>
    // - шаги должны быть переиспользованы

    // 1. Очистить информацию о сохраненных шагах предыдущей операции данного типа (out from there)
    // 2. Получить информацию для формирования списка первого шага (input)
    // 3. Логика по формированию:
    // 3.1 текста сообщения (output)
    // 3.2 информация для построения клавиатуры, для следующего шага или результата операции.
    // В первом случае это array of pair, где первое - это наименование inline кнопки,
    // второе - это идентификатор указывающий на номер выбранной позиции списка собранных данных (output)
    // Во втором случае это count of items типа integer, сообщающий о количестве (output)
    // 3.3 признак того что в результате из п.3.2. Возможно не нужно! (output)
    // 3.4 хранилище ключ значение (map) типа <navigateType,List> содержащее информацию по итерированию для полученного callback (output)
    // 3.5 тип клавиатуры
    // 3.6 признак наличия результата
    // 4. Сохранить информацию о шаге
    override fun init(chatId: Long, user: User): BotApiMethod<out Serializable> {
        stepStore.remove(chatId)
        operationResultStore.remove(chatId)

        val userGroups = quizletService.userGroups(chatId, userContext(user))

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
//            user.operationData = objectMapper.writeValueAsString(OperationData(BotService.MainMenu.STUDIED.title, value))
//            accountRepository.save(account)

            // build text message with keyboard
            messageFormatter.buildStepPageKeyboardMessage(chatId, text, outputData)
        } else {
            SendMessage().setChatId(chatId).setText("User classes doesn't find")
        }
    }

    override fun execute(chatId: Long,
                         messageId: Int,
                         callData: String,
                         user: User): BotApiMethod<out Serializable> {

        val (navigateTypeName, data) = callData.split(";")

        val navigateType = NavigateType.values().firstOrNull { it.name == navigateTypeName }
                ?: throw RuntimeException("unexpected navigateType=$navigateTypeName")

        val activeStep = stepStore[chatId] ?: return buildCleanEditMessage(chatId, messageId)

        return when (activeStep.stepType) {
            GROUP -> handleSelectGroup(chatId, messageId, callData, navigateType, activeStep)

            SET -> handleSelectSet(chatId, messageId, callData, navigateType, activeStep, user)

            RESULT -> handleSelectResult(chatId, messageId, callData, navigateType)
        }
    }

    fun handleSelectGroup(chatId: Long,
                          messageId: Int,
                          callData: String,
                          navigateType: NavigateType,
                          activeStep: ActiveStep): BotApiMethod<out Serializable> {
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
            PAGING_BY_BUTTONS -> TODO() // need to implement when groups (classes) will be include more than 4 items
            else -> return buildCleanKeyboardMessage(chatId, messageId)
        }
    }

    private fun handleSelectSet(chatId: Long,
                                messageId: Int,
                                callData: String,
                                navigateType: NavigateType,
                                activeStep: ActiveStep,
                                user: User): BotApiMethod<out Serializable> {
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

                val statistics = quizletService.studiedInfo(chatId, group.id, setIds, activeStep.userGroups, userContext(user))

                stepStore[chatId] = ActiveStep(RESULT, activeStep.userGroups, group.id)

                operationResultStore[chatId] = OperationResult(statistics)

                // build text message for first element
                val text = createMessageText(statistics.groupName, statistics.setsStats[0])

                messageFormatter.buildItemPageKeyboardMessage(chatId, messageId, text, statistics.setsStats.size)
            }
            PAGING_BY_BUTTONS -> {
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
            PAGING_BY_ITEMS -> {
                val operationResult = operationResultStore[chatId]
                        ?: throw RuntimeException("not available command=\"$command\" for step")

                val countOfItems = operationResult.studied.setsStats.size
                val selectedItem = value.toInt()

                val text = createMessageText(operationResult.studied.groupName, operationResult.studied.setsStats[selectedItem - 1])

                operationResultStore[chatId] = OperationResult(operationResult.studied)

                messageFormatter.buildItemPageKeyboardMessage(chatId, messageId, text, countOfItems, selectedItem)
            }
            // TODO implement it. Show all items or only not studied (less the 6 executed modes or check enabled modes in user settings)
            UPDATE_ITEMS -> TODO()
            else -> return buildCleanKeyboardMessage(chatId, messageId)
        }
    }

    private fun buildCleanEditMessage(chatId: Long, messageId: Int): BotApiMethod<out Serializable> {
        return EditMessageText()
                .setChatId(chatId)
                .setMessageId(messageId)
                .enableMarkdown(true)
                .setText("*Please, continue the further new operation.*")
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