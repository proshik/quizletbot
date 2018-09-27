package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import ru.proshik.english.quizlet.telegramBot.service.command.StatisticCommand
import ru.proshik.english.quizlet.telegramBot.service.model.ModeType
import ru.proshik.english.quizlet.telegramBot.service.model.Statistics
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap

@Service
class QuizletOperationService(private val usersService: UsersService,
                              private val authenticationService: AuthenticationService,
                              private val quizletInfoService: QuizletInfoService) {

    companion object {
        private const val STATISTIC_COMMAND = "Statistics"
        private const val NOTIFICATIONS_COMMAND = "Notifications"
        private const val ACCOUNT_COMMAND = "Account"

        private val COMMANDS = listOf(STATISTIC_COMMAND, NOTIFICATIONS_COMMAND, ACCOUNT_COMMAND)
    }

    // TODO when I will read about generict in the kotlin I change generic types
//    val operationQueue = ConcurrentHashMap<Long, Operation<*, *>>()

    val commandQueue = ConcurrentHashMap<Long, StatisticCommand>()

    val cacheCommand = ConcurrentHashMap<Long, Pair<Int, Statistics>>()

    fun connectToQuizlet(chatId: Long): String {
        val user = usersService.getUser(chatId.toString())
        if (user == null) {
            usersService.create(chatId.toString())
        }

        return authenticationService.generateAuthUrl(chatId.toString())
    }

    fun handleCommand(chatId: Long, text: String, messageId: Int? = null): BotApiMethod<out Serializable> {
        val activeUserCommand = commandQueue[chatId]

        if (activeUserCommand == null) {
            return when (text) {
                STATISTIC_COMMAND -> {
                    // clear the cache of result statistic the previous operation
                    cacheCommand.remove(chatId)

                    val initCommand = initStatic(chatId);
                    formatStep(chatId, initCommand.first, initCommand.second)
                }
                else -> SendMessage().setChatId(chatId).setText("Select Operation")
                        .setReplyMarkup(buildDefaultKeyboard())
            }
        } else {
            val items: MutableList<String> = ArrayList()
            when (activeUserCommand.currentStep) {
                StatisticCommand.Step.SELECT_GROUP -> {
                    val userGroups = quizletInfoService.userGroups(chatId)

                    val group = userGroups.asSequence().filter { group -> group.name == text }.firstOrNull()
                    if (group == null) {
                        commandQueue.remove(chatId)
                        return SendMessage().setChatId(chatId).setText("Doesn't find group for $text")
                                .setReplyMarkup(buildDefaultKeyboard())
                    }

                    for (set in group.sets.sortedByDescending { set -> set.publishedDate }) {
                        items.add(set.title)
                    }

                    if (items.isEmpty()) {
                        commandQueue.remove(chatId)
                        return SendMessage().setChatId(chatId)
                                .setText("Doesn't find not one set for ${group.name}")
                                .setReplyMarkup(buildDefaultKeyboard())
                    }

                    activeUserCommand.groupId = group.id
                    activeUserCommand.currentStep = StatisticCommand.Step.SELECT_SET

                    commandQueue[chatId] = activeUserCommand

                    return formatStep(chatId, items, true, false, messageId)
                }
                StatisticCommand.Step.SELECT_SET -> {
                    val userGroups = quizletInfoService.userGroups(chatId)

                    val group = userGroups.asSequence()
                            .filter { group -> group.id == activeUserCommand.groupId }
                            .first()

                    val setIds = if (text == "All") {
                        group.sets.map { set -> set.id }
                    } else {
                        group.sets.asSequence()
                                .filter { set -> set.title == text }
                                .map { set -> set.id }
                                .toList()
                    }

                    if (setIds.isEmpty()) {
                        commandQueue.remove(chatId)
                        return SendMessage()
                                .setChatId(chatId)
                                .setText("Incorrect request. The operation will start from the beginning")
                                .setReplyMarkup(buildDefaultKeyboard())
                    }

                    val statistics = quizletInfoService.buildStatistic(chatId, group.id, setIds, userGroups)

                    cacheCommand[chatId] = Pair(1, statistics)

                    commandQueue.remove(chatId)

                    return formatResult(chatId, statistics, messageId!!)
                }
                else -> throw RuntimeException("unexpected")
            }
        }
    }

    fun handleCallback(chatId: Long, messageId: Int, callData: String): BotApiMethod<out Serializable> {
        val statistics = cacheCommand[chatId]
        if (statistics != null) {
            if (callData == "previous") {
                val ms: String
                if (statistics.first > 1) {
                    val set = statistics.second.setsStats[statistics.first - 1]

                    val res = java.lang.StringBuilder("Set: *${set.title}*\n")

                    val modeStatByMode = set.modeStats
                            .groupBy { modeStat -> modeStat.mode }
                            .toMap()

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
                    // TODO url must be uncomment in the all places
//                    res.append(set.url)
                    res.append("\n")

                    ms = res.toString()

                    cacheCommand[chatId] = Pair(statistics.first - 1, statistics.second)

                } else {
                    ms = "null"
                }

                return EditMessageText()
                        .setChatId(chatId)
                        .setParseMode(ParseMode.MARKDOWN)
                        .setReplyMarkup(buildInlineKeyboardMarkup())
//                        .setReplyMarkup(buildInlineKeyboardMarkup1())
                        .setMessageId(messageId)
                        .setText(ms)
            } else if (callData == "next") {
                val ms: String
                if (statistics.first < statistics.second.setsStats.size - 1) {
                    val set = statistics.second.setsStats[statistics.first + 1]

                    val res = java.lang.StringBuilder("Set: *${set.title}*\n")

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
//                    res.append(set.url)
                    res.append("\n")

                    ms = res.toString()

                    cacheCommand[chatId] = Pair(statistics.first + 1, statistics.second)
                } else {
                    ms = "null"
                }


                return EditMessageText()
                        .setChatId(chatId)
                        .setParseMode(ParseMode.MARKDOWN)
                        .setReplyMarkup(buildInlineKeyboardMarkup())
//                        .setReplyMarkup(buildInlineKeyboardMarkup1())
                        .setMessageId(messageId)
                        .setText(ms)
            } else {
                return SendMessage().setChatId(chatId).setText("Callback message")
            }
        }

        val activeUserCommand = commandQueue[chatId]
        if (activeUserCommand != null) {
            val items: MutableList<String> = ArrayList()
            when (activeUserCommand.currentStep) {
                StatisticCommand.Step.SELECT_GROUP -> {
                    val userGroups = quizletInfoService.userGroups(chatId)

                    val group = userGroups.asSequence().filter { group -> group.name == callData }.firstOrNull()
                    if (group == null) {
                        commandQueue.remove(chatId)
                        return SendMessage().setChatId(chatId).setText("Doesn't find group for $callData")
                                .setReplyMarkup(buildDefaultKeyboard())
                    }

                    for (set in group.sets.sortedByDescending { set -> set.publishedDate }) {
                        items.add(set.title)
                    }

                    if (items.isEmpty()) {
                        commandQueue.remove(chatId)
                        return SendMessage().setChatId(chatId).setText("Doesn't find not one set for ${group.name}")
                                .setReplyMarkup(buildDefaultKeyboard())
                    }

                    activeUserCommand.groupId = group.id
                    activeUserCommand.currentStep = StatisticCommand.Step.SELECT_SET

                    commandQueue[chatId] = activeUserCommand

                    return formatStep(chatId, items, true, true, messageId)
                }
                StatisticCommand.Step.SELECT_SET -> {
                    val userGroups = quizletInfoService.userGroups(chatId)

                    val group = userGroups.asSequence().filter { group -> group.id == activeUserCommand.groupId }.first()

                    val setIds = if (callData == "All") {
                        group.sets.map { set -> set.id }
                    } else {
                        group.sets.asSequence().filter { set -> set.title == callData }.map { set -> set.id }.toList()
                    }

                    if (setIds.isEmpty()) {
                        commandQueue.remove(chatId)
                        return SendMessage().setChatId(chatId).setText("Incorrect request. The operation will start from the beginning")
                                .setReplyMarkup(buildDefaultKeyboard())
                    }

                    val s = quizletInfoService.buildStatistic(chatId, group.id, setIds, userGroups)

                    cacheCommand[chatId] = Pair(1, s)

                    commandQueue.remove(chatId)

                    return formatResult(chatId, s, messageId)
                }
                else -> throw RuntimeException("unexpected")

            }
        }

        cacheCommand.remove(chatId)
        return EditMessageReplyMarkup().setChatId(chatId).setMessageId(messageId).setReplyMarkup(null)
    }

    private fun initStatic(chatId: Long): Pair<List<String>, Boolean> {
        val command = StatisticCommand(chatId)

        val userGroups = quizletInfoService.userGroups(chatId)

        val items: MutableList<String> = ArrayList()
        var setStep = false
        when {
            userGroups.isEmpty() -> return Pair(emptyList(), false)
            userGroups.size == 1 -> {
                for (set in userGroups[0].sets.sortedByDescending { set -> set.publishedDate }) {
                    items.add(set.title)
                }

                command.groupId = userGroups[0].id
                command.currentStep = StatisticCommand.Step.SELECT_SET
                setStep = true
            }
            else -> {
                for (group in userGroups) {
                    items.add(group.name)
                }

                command.currentStep = StatisticCommand.Step.SELECT_GROUP
            }
        }

        commandQueue[chatId] = command

        return Pair(items, setStep)
    }

    private fun formatStep(chatId: Long, items: List<String>,
                           all: Boolean = false,
                           new: Boolean = false,
                           messageId: Int? = null): BotApiMethod<out Serializable> {

        val markupInline = InlineKeyboardMarkup()

        val rows = ArrayList<List<InlineKeyboardButton>>()
        // Set the keyboard to the markup
        if (all) {
            val rowInline = ArrayList<InlineKeyboardButton>()
            rowInline.add(InlineKeyboardButton().setText("All").setCallbackData("All"))
            rows.add(rowInline)
        }

        var i = 1
        var row = ArrayList<InlineKeyboardButton>()
        for (item in items) {

            row.add(InlineKeyboardButton().setText(item).setCallbackData(item))

            if (i % 1 == 0) {
                rows.add(row)
                i = 1
                row = ArrayList()
            } else {
                i++
            }
        }

        rows.add(row)

        markupInline.keyboard = rows
        if (new) {
            return EditMessageReplyMarkup().setChatId(chatId).setReplyMarkup(markupInline).setMessageId(messageId)
        } else {
            return SendMessage().setChatId(chatId).setText("Select Item:").setReplyMarkup(markupInline)
        }
    }

    private fun formatResult(chatId: Long, statistics: Statistics, messageId: Int): BotApiMethod<out Serializable> {
        val res = buildFinalREs(statistics)

        val message = EditMessageText()
                .enableMarkdown(true)
                .setChatId(chatId)
                .setMessageId(messageId)
//                .setReplyMarkup(buildDefaultKeyboard())
                .setText(res.toString())

        if (statistics.setsStats.size > 1) {
            message.replyMarkup = buildInlineKeyboardMarkup()
        }

        return message
    }

    fun buildFinalREs(statistics: Statistics): String {
        val res = StringBuilder("Group: *${statistics.groupName}*\n\n")

        val sets = statistics.setsStats.sortedByDescending { set -> set.publishedDate }

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

    private fun buildInlineKeyboardMarkup(): InlineKeyboardMarkup {
        val markupInline = InlineKeyboardMarkup()
        val rowsInline = ArrayList<List<InlineKeyboardButton>>()

        val rowInline = ArrayList<InlineKeyboardButton>()
        rowInline.add(InlineKeyboardButton().setText("previous").setCallbackData("previous"))
        rowInline.add(InlineKeyboardButton().setText("next").setCallbackData("next"))

        // Set the keyboard to the markup
        rowsInline.add(rowInline)

        // Add it to the message
        markupInline.keyboard = rowsInline
        return markupInline
    }

    private fun buildDefaultKeyboard(): ReplyKeyboardMarkup {
        val keyboardMarkup = ReplyKeyboardMarkup()
        keyboardMarkup.resizeKeyboard = true

        val rows = ArrayList<KeyboardRow>()

        val statistics = KeyboardRow()
        statistics.add("Statistics")

        val notifications = KeyboardRow()
        notifications.add("Notifications")

        val account = KeyboardRow()
        account.add("Account")

        rows.add(statistics)
        rows.add(notifications)
        rows.add(account)

        keyboardMarkup.keyboard = rows

        return keyboardMarkup
    }

//    fun handleCommand(chatId: Long, text: String): BotApiMethod<out Serializable> {
//        val operation = operationQueue[chatId]
//
//        if (operation != null) {
//            val operationHandle = operation.navigate(text)
//            // TODO remove cast after transform to kotlin code
//            return operation.formatter.format(operationHandle)
//        }
//
//        val operationEvent = operationResolver(chatId, text)
//        if (operationEvent == null) {
//            // TODO change text
//            return SendMessage().setChatId(chatId).setText("Default message like \"use the keyboard and bla-bla-bla\"")
//        } else {
//            val initResult = operationEvent.init()
//
//            return operationEvent.formatter.format(initResult)
//        }
//    }
//
//    private fun operationResolver(chatId: Long, text: String): Operation<*, *>? {
//        return when (text) {
//            // TODO exclude StaticOperationProvider from Operation. Need to do a operation as light as possible
//            STATISTIC_COMMAND -> StatisticOperation(chatId, StaticOperationProvider(quizletInfoService))
//            else -> null
//        }
//    }

}
