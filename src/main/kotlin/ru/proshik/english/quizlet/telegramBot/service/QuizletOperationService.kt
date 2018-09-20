package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import ru.proshik.english.quizlet.telegramBot.service.command.StatisticCommand
import ru.proshik.english.quizlet.telegramBot.service.model.ModeType
import ru.proshik.english.quizlet.telegramBot.service.model.Statistics
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

    val cacheCommand = HashMap<Long, Statistics>()

    fun connectToQuizlet(chatId: Long): String {
        val user = usersService.getUser(chatId.toString())
        if (user == null) {
            usersService.create(chatId.toString())
        }

        return authenticationService.generateAuthUrl(chatId.toString())
    }

    fun handleCommand(chatId: Long, text: String): BotApiMethod<Message> {
        val command = commandQueue[chatId]
        if (command != null) {
            val items: MutableList<String> = ArrayList()
            when (command.currentStep) {
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
                        return SendMessage().setChatId(chatId).setText("Doesn't find not one set for ${group.name}")
                                .setReplyMarkup(buildDefaultKeyboard())
                    }

                    command.groupId = group.id
                    command.currentStep = StatisticCommand.Step.SELECT_SET

                    commandQueue[chatId] = command

                    return formatStep(chatId, items, true)
                }
                StatisticCommand.Step.SELECT_SET -> {
                    val userGroups = quizletInfoService.userGroups(chatId)

                    val group = userGroups.asSequence().filter { group -> group.id == command.groupId }.first()

                    val setIds = if (text == "All") {
                        group.sets.map { set -> set.id }
                    } else {
                        group.sets.asSequence().filter { set -> set.title == text }.map { set -> set.id }.toList()
                    }

                    if (setIds.isEmpty()) {
                        commandQueue.remove(chatId)
                        return SendMessage().setChatId(chatId).setText("Incorrect request. The operation will start from the beginning")
                                .setReplyMarkup(buildDefaultKeyboard())
                    }

                    val statistics = quizletInfoService.buildStatistic(chatId, group.id, setIds, userGroups)

                    cacheCommand[chatId] = statistics

                    commandQueue.remove(chatId)

                    return formatResult(chatId, statistics)
                }
                else -> throw RuntimeException("unexpected")
            }
        } else {
            return when (text) {
                STATISTIC_COMMAND -> {
                    // clear the cache of result statistic the previous operation
                    cacheCommand.remove(chatId)

                    val initCommand = initStaticCommand(chatId);
                    formatStep(chatId, initCommand.first, initCommand.second)
                }
                else -> SendMessage().setChatId(chatId).setText("Select Operation")
                        .setReplyMarkup(buildDefaultKeyboard())
            }

        }
    }

    fun handleCallback(chatId: Long, messageId: Long, callData: String): BotApiMethod<Message> {
        val statistics = cacheCommand[chatId]

        return SendMessage().setChatId(chatId).setText("Callback message")
    }

    private fun initStaticCommand(chatId: Long): Pair<List<String>, Boolean> {
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

    private fun formatStep(chatId: Long, items: List<String>, all: Boolean = false): BotApiMethod<Message> {
        val message = SendMessage().setChatId(chatId)

        val keyboardMarkup = ReplyKeyboardMarkup()
        keyboardMarkup.oneTimeKeyboard = true

        val rows = ArrayList<KeyboardRow>()

        if (all) {
            val allRow = KeyboardRow()
            allRow.add("All")
            rows.add(allRow)
        }

        for (item in items) {
            val row = KeyboardRow()
            row.add(item)
            rows.add(row)
        }

        keyboardMarkup.keyboard = rows

        message.replyMarkup = keyboardMarkup
        message.text = "Select item:"

        return message
    }

    private fun formatResult(chatId: Long, statistics: Statistics): BotApiMethod<Message> {
        val res = StringBuilder("Group: *${statistics.groupName}*\n\n")

        for (set in statistics.setsStats.sortedByDescending { set -> set.publishedDate }) {
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
            res.append(set.url)
            res.append("\n")
        }

        val message = SendMessage()
                .enableMarkdown(true)
                .setChatId(chatId)
//                .setReplyMarkup(buildDefaultKeyboard())
                .setText(res.toString())

        if (statistics.setsStats.size > 1){
            message.replyMarkup = buildInlineKeyboardMarkup()
        }

        return message
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

//    fun handleCommand(chatId: Long, text: String): BotApiMethod<Message> {
//        val operation = operationQueue[chatId]
//
//        if (operation != null) {
//            val operationHandle = operation.nextStep(text)
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
