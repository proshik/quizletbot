package ru.proshik.english.quizlet.telegramBot.service.operation

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import ru.proshik.english.quizlet.telegramBot.dto.UserGroupsResp
import ru.proshik.english.quizlet.telegramBot.service.MessageBuilder
import ru.proshik.english.quizlet.telegramBot.service.MessageBuilder.Companion.ALL_ITEMS
import ru.proshik.english.quizlet.telegramBot.service.QuizletService
import ru.proshik.english.quizlet.telegramBot.service.operation.StudiedOperationExecutor.StepType.*
import ru.proshik.english.quizlet.telegramBot.service.vo.*
import ru.proshik.english.quizlet.telegramBot.service.vo.NavigateType.*
import java.io.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap


@Component
class StudiedOperationExecutor(val quizletService: QuizletService) : OperationExecutor {

    companion object {

        val DATA_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy")

        val RESULT_ADDIT_BUTTONS = listOf(Pair("All", ALL_ITEMS), Pair("> 5", "5"), Pair("> 3", "3"), Pair("> 2", "2"))
    }

    val messageFormatter = MessageBuilder()

    enum class StepType {
        GROUP,
        SET,
        RESULT
    }

    data class StepInfo(val stepType: StepType,
                        val userGroups: List<UserGroupsResp>,
                        val groupId: Long?,
                        val studied: Studied? = null,
                        val studiedSet: List<StudiedSet>? = null)

    private val stepStore = ConcurrentHashMap<Long, StepInfo>()

    // Notes:
    // - callback format: <operationType>;<stepType>;<navigateType>;<itemId>
    // - шаги должны быть переиспользованы
    // - шаги могут наследоваться от разных интерфейсов. Это может быть реализация обработки callback ответа, а может для обычного сообщения

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
    override fun init(chatId: Long): BotApiMethod<out Serializable> {
        stepStore.remove(chatId)

        val data = quizletService.userGroups(chatId)

        return when {
            data.size > 1 -> {
                val text = "*Please, select a class:*\n"
                val items = data.asSequence()
                        .map { group -> Pair(group.name, group.id.toString()) }
                        .toList()
                val prefix = "${OperationType.STUDIED.name};${GROUP.name}"

                // save step information
                stepStore[chatId] = StepInfo(GROUP, data, null)
                // build text message with keyboard
                messageFormatter.buildStepPageKeyboardMessage(chatId, text, items, prefix)
            }
            data.size == 1 -> {
                val text = "*Please, select a set(s):*\n"
                val items = data.first().sets.asSequence()
                        .map { set -> Pair(set.title, set.id.toString()) }
                        .toList()
                val prefix = "${OperationType.STUDIED.name};${SET.name}"

                // save step information
                stepStore[chatId] = StepInfo(SET, data, data.first().id)
                // build text message with keyboard
                messageFormatter.buildStepPageKeyboardMessage(chatId, text, items, prefix)
            }
            else -> SendMessage().setChatId(chatId).setText("Users classes doesn't find")
        }
    }

    override fun execute(chatId: Long,
                         messageId: Int,
                         stepType: String,
                         navType: NavigateType,
                         value: String): BotApiMethod<out Serializable> {

        return when (StepType.valueOf(stepType)) {
            GROUP -> handleSelectGroup(chatId, messageId, navType, value)
            SET -> handleSelectSet(chatId, messageId, navType, value)
            RESULT -> handleSelectResult(chatId, messageId, navType, value)
        }
    }

    fun handleSelectGroup(chatId: Long,
                          messageId: Int,
                          navigateType: NavigateType,
                          value: String): BotApiMethod<out Serializable> {
        val stepInfo = stepStore[chatId] ?: return buildCleanEditMessage(chatId, messageId)

        return when (navigateType) {
            NEXT_STEP -> {
                val group = stepInfo.userGroups.asSequence()
                        .filter { it.id.toString() == value }
                        .firstOrNull()
                if (group == null) {
                    stepStore.remove(chatId)
                    return buildCleanEditMessage(chatId, messageId)
                }

                val items = group.sets.asSequence()
                        .map { it -> Pair(it.title, it.id.toString()) }
                        .toList()

                if (items.isEmpty()) {
                    stepStore.remove(chatId)
                    return EditMessageText()
                            .setChatId(chatId)
                            .setMessageId(messageId)
                            .setText("Class *${group.name}* doesn't contain any sets.")
                            .enableMarkdown(true)
                            .setReplyMarkup(null)
                }

                stepStore[chatId] = StepInfo(SET, stepInfo.userGroups, group.id)

                val text = StringBuilder("Class: [${group.name}](${group.url}\n\n")
                text.append("*Please, select a set from class:*")

                val prefix = "${OperationType.STUDIED.name};${SET.name}"

                val prevStep = stepInfo.userGroups.size > 1

                messageFormatter.buildStepPageKeyboardMessage(chatId, text.toString(), items, prefix,
                        messageId, prevStep = prevStep, showAllLine = true)
            }
            PAGING_BY_BUTTONS -> {
                val items = stepInfo.userGroups.asSequence()
                        .map { group -> Pair(group.name, group.id.toString()) }
                        .toList()
                val text = "*Please, select a class:*\n"

                val prefix = "${OperationType.STUDIED.name};${GROUP.name}"

                messageFormatter.buildStepPageKeyboardMessage(chatId, text, items, prefix, messageId,
                        firstElemInGroup = value.toInt(), pagingButton = true)
            }
            else -> return buildCleanKeyboardMessage(chatId, messageId)
        }
    }

    private fun handleSelectSet(chatId: Long,
                                messageId: Int,
                                navigateType: NavigateType,
                                value: String): BotApiMethod<out Serializable> {

        val stepInfo = stepStore[chatId] ?: return buildCleanEditMessage(chatId, messageId)

        return when (navigateType) {
            PREV_STEP -> {
                val data = quizletService.userGroups(chatId)

                val text = "*Please, select a class:*\n"
                val stepType = GROUP
                val outputData = data.asSequence()
                        .map { group -> Pair(group.name, group.id.toString()) }
                        .toList()
                val prefix = "${OperationType.STUDIED.name};${GROUP.name}"
                val prevStep = false

                // save information about active step
                stepStore[chatId] = StepInfo(stepType, data, null)
                // build text message with keyboard
                messageFormatter.buildStepPageKeyboardMessage(chatId, text, outputData, prefix, messageId, prevStep = prevStep)
            }
            NEXT_STEP -> {
                val group = stepInfo.userGroups.asSequence()
                        .filter { group -> group.id == stepInfo.groupId }
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

                val studied = quizletService.studiedInfo(chatId, group.id, setIds, stepInfo.userGroups)

                stepStore[chatId] = StepInfo(RESULT, stepInfo.userGroups, group.id, studied, studied.setsStats)

                // build text message for first element
                val text = createMessageText(studied.studiedClass, studied.setsStats[0])

                val prefix = "${OperationType.STUDIED.name};${RESULT.name}"

                val items = studied.setsStats.map { it.id }

                messageFormatter.buildItemPageKeyboardMessage(chatId, messageId, text, items, prefix,
                        additionalItems = RESULT_ADDIT_BUTTONS)
            }
            PAGING_BY_BUTTONS -> {
                val group = stepInfo.userGroups.asSequence()
                        .filter { group -> group.id == stepInfo.groupId }
                        .first()

                val text = "Class: *${group.name}*\n Please, select a set from the class *${group.name}:*\n"

                val items = group.sets.asSequence()
                        .map { it -> Pair(it.title, it.id.toString()) }
                        .toList()

                val prefix = "${OperationType.STUDIED.name};${SET.name}"

                val prevStep = stepInfo.userGroups.size > 1

                messageFormatter.buildStepPageKeyboardMessage(chatId, text, items, prefix, messageId,
                        firstElemInGroup = value.toInt(), pagingButton = true, showAllLine = true, prevStep = prevStep)
            }
            else -> return buildCleanKeyboardMessage(chatId, messageId)
        }
    }

    private fun handleSelectResult(chatId: Long,
                                   messageId: Int,
                                   navigateType: NavigateType,
                                   value: String): BotApiMethod<out Serializable> {
        val stepInfo = stepStore[chatId] ?: return buildCleanEditMessage(chatId, messageId)

        val studied = stepInfo.studied ?: throw RuntimeException("incorrect state")
        val studiedSets = stepInfo.studiedSet ?: throw RuntimeException("incorrect state")
        // paging by result
        return when (navigateType) {
            PAGING_BY_ITEMS -> {
                val selectedItem = value.toLong()

                val set = studied.setsStats.first { it.id == selectedItem }

                val text = createMessageText(studied.studiedClass, set)

                val prefix = "${OperationType.STUDIED.name};${RESULT.name}"

                val items = studiedSets.map { it.id }

                messageFormatter.buildItemPageKeyboardMessage(chatId, messageId, text, items, prefix,
                        studiedSets.indexOf(set) + 1, additionalItems = RESULT_ADDIT_BUTTONS)
            }
            UPDATE_ITEMS -> {
                val newSets = if (value == ALL_ITEMS) {
                    studied.setsStats.asSequence().map { it }.toList()
                } else {
                    val count = value.toInt()
                    studied.setsStats.asSequence()
                            .map { Pair(it, it.studiedModes) }
                            .filter { it ->
                                it.second.asSequence()
                                        .distinctBy { it.type }
                                        .filter { it.finishDate != null }
                                        .count() > count
                            }
                            .toMap().keys.toList()
                }

                val text = createMessageText(studied.studiedClass, newSets[0])

                val prefix = "${OperationType.STUDIED.name};${RESULT.name}"

                val items = newSets.map { it.id }

                stepStore[chatId] = StepInfo(stepInfo.stepType, stepInfo.userGroups, stepInfo.groupId, studied, newSets)

                messageFormatter.buildItemPageKeyboardMessage(chatId, messageId, text, items, prefix,
                        additionalItems = RESULT_ADDIT_BUTTONS)
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
    }

    private fun buildCleanKeyboardMessage(chatId: Long, messageId: Int): BotApiMethod<out Serializable> {
        return EditMessageReplyMarkup()
                .setChatId(chatId)
                .setMessageId(messageId)
                .setReplyMarkup(null)
    }

    private fun createMessageText(studiedClass: StudiedClass, set: StudiedSet): String {
        val message = StringBuilder()
        message.append("Class: *${studiedClass.name}*\n")
        message.append("Set: *${set.title}*\n\n")

        val modeStatByMode = set.studiedModes.groupBy { it.type }

        for (mode in ModeType.values()) {
            val modeStats = modeStatByMode[mode]
            if (modeStats != null) {
                val modeStat = modeStats.asSequence()
                        .sortedWith(compareBy({ it.finishDate }, { it.startDate }))
                        .first()

                val text = if (modeStat.finishDate != null) {
                    val finishedDate = Instant.ofEpochMilli(modeStat.finishDate * 1000L).atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    "Finished *${mode.title}* on ${DATA_FORMAT.format(finishedDate)} " +
//                            "(${modeStats.size}) " +
                            "✅"

                } else {
                    val startedDate = Instant.ofEpochMilli(modeStat.startDate * 1000L).atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    "Started *${mode.title}* on ${DATA_FORMAT.format(startedDate)} ☑️"
                }

                message.append(text)

                // add formatted score
//                if (modeStat.formattedScore != null) message.append(" ${modeStat.formattedScore}")

                message.append("\n")
            } else {
                message.append("*${mode.title}* has no activity ❌\n")
            }
        }

        message.append("\n_Go study:_ [${set.title.replace("[", "").replace("]", "")}](${set.url})")

        return message.toString()
    }

}