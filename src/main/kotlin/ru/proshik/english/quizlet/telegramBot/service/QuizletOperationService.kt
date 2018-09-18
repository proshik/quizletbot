package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import ru.proshik.english.quizlet.telegramBot.dto.SetResp
import ru.proshik.english.quizlet.telegramBot.service.command.StatisticCommand
import ru.proshik.english.quizlet.telegramBot.service.model.Statistics
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors

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

                    val group = userGroups.first { group -> group.name == text }
                    for (set in group.sets.sortedByDescending { set -> set.publishedDate }) {
                        items.add(set.title)
                    }

                    if (items.isEmpty()) {
                        commandQueue.remove(chatId)
                        return SendMessage().setChatId(chatId).setText("Doesn't find not one set for ${group.name}")
                                .setReplyMarkup(sendCustomKeyboard(chatId))
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
                                .setReplyMarkup(sendCustomKeyboard(chatId))
                    }

                    val statistics = quizletInfoService.buildStatistic(chatId, group.id, setIds, userGroups)

                    commandQueue.remove(chatId)

                    return formatResult(chatId, statistics)
                }
                else -> throw RuntimeException("unexpected")
            }
        } else {
            return when (text) {
                STATISTIC_COMMAND -> {
                    val initCommand = initStaticCommand(chatId);
                    formatStep(chatId, initCommand.first, initCommand.second)
                }
                else -> SendMessage().setChatId(chatId).setText("Select Operation")
                        .setReplyMarkup(sendCustomKeyboard(chatId))
            }

        }
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

    fun formatStep(chatId: Long, items: List<String>, all: Boolean = false): BotApiMethod<Message> {
        val message = SendMessage().setChatId(chatId)

        val keyboardMarkup = ReplyKeyboardMarkup()
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

    fun formatResult(chatId: Long, statistics: Statistics): BotApiMethod<Message> {
        return SendMessage()
                .setChatId(chatId)
                .setReplyMarkup(sendCustomKeyboard(chatId))
                .setText("Result for group done")
    }

    fun sendCustomKeyboard(chatId: Long): ReplyKeyboardMarkup {
        val keyboardMarkup = ReplyKeyboardMarkup()

        val rows = java.util.ArrayList<KeyboardRow>()

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

//    fun handleOperation(chatId: Long, text: String): BotApiMethod<Message> {
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
