package ru.proshik.english.quizlet.telegramBot.service

import org.apache.log4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.updateshandlers.SentCallback
import java.io.Serializable
import java.util.*

@Component
class TelegramBotService(@Value("\${telegram.token}") private val token: String,
                         @Value("\${telegram.username}") private val username: String,
                         private val quizletInfoService: QuizletInfoService,
                         private val quizletOperationService: QuizletOperationService) : TelegramLongPollingBot() {

    companion object {

        private val LOG = Logger.getLogger(TelegramBotService::class.java)

        private const val DEFAULT_MESSAGE = "This bot can help you get information about studied sets on quizlet.com"
    }

    override fun onUpdateReceived(update: Update) {
        val response = onWebHookUpdateReceived(update)
        try {
            execute(response)
        } catch (e: TelegramApiException) {
            LOG.error("Panic! Messages not sending!", e)
        }
    }

    override fun getBotUsername(): String {
        return username
    }

    override fun getBotToken(): String {
        return token
    }

    fun onWebHookUpdateReceived(update: Update): BotApiMethod<Message> = when {
        update.hasMessage() -> message(update)
        update.hasCallbackQuery() -> callback(update)
        else -> throw RuntimeException("Unexpected situation")
    }

    private fun message(update: Update): BotApiMethod<Message> = when {
        update.message.isCommand -> commandMessage(update)
        else -> operationMessage(update)
    }

    private fun callback(update: Update): BotApiMethod<Message> {
        return SendMessage().setChatId(update.callbackQuery.message.chatId).setText("Callback message")
    }

    private fun commandMessage(update: Update): BotApiMethod<Message> {
        return when (update.message.text.split(" ")[0]) {
            "/start" -> SendMessage().setChatId(update.message?.chatId).setText(DEFAULT_MESSAGE)
            "/help" -> SendMessage().setChatId(update.message?.chatId).setText(DEFAULT_MESSAGE)
            "/connect" -> {
                val chatId = update.message?.chatId
                val quizletConnectUrl = quizletOperationService.connectToQuizlet(chatId!!)

                SendMessage().setChatId(update.message?.chatId).setText(quizletConnectUrl)
            }
            else -> throw RuntimeException("Unexpected command")
        }
    }

    private fun operationMessage(update: Update): BotApiMethod<Message> {
        val chatId = update.message.chatId
        val text = update.message.text

        return quizletOperationService.handleOperation(chatId, text)
//
//
//        val command = commandQueue[chatId]
//
//        try {
//            if (command != null) {
//                val message = SendMessage().setChatId(chatId)
//                if (command.groupId == null) {
//                    val group = command.meta.asSequence().filter { group -> group.name == update.message.text }.first()
//                    val keyboardMarkup = ReplyKeyboardMarkup()
//                    val rows = ArrayList<KeyboardRow>()
//                    for (set in group.sets) {
//                        val row = KeyboardRow()
//                        row.add(set.title)
//                        rows.add(row)
//                    }
//                    // Set the keyboard to the markup
//                    keyboardMarkup.keyboard = rows
//                    // Add it to the message
//                    message.replyMarkup = keyboardMarkup
//
//                    command.groupId = group.id
//                    commandQueue.put(chatId, command)
//                } else if (command.setIds == null) {
//                    val set = command.meta.asSequence()
//                            .filter { group -> group.id == command.groupId }
//                            .flatMap { group -> group.sets.asSequence() }
//                            .filter { set -> set.title == update.message.text }
//                            .first()
//
//                    val statistic = quizletInfoService.buildStatistic(chatId, command.groupId, listOf(set.id), command.meta)
//                    val mapper = ObjectMapper()
//                    message.text = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(statistic)
//
//                    commandQueue.remove(chatId)
//                } else {
//
//                    commandQueue.remove(chatId)
//                    throw RuntimeException("weird behavior")
//                }
//
//                return message
//            } else {
//                return when (update.message.text) {
//                    STATISTIC_COMMAND -> buildStartMessageCommand(chatId, quizletInfoService.userGroups(chatId))
//                    else -> defaultMessage(update)
//                }
//            }
//        } catch (e: Exception) {
//            commandQueue.remove(chatId)
//            LOG.error("unexpected error", e)
//        }

        return SendMessage().setChatId(chatId).setText("Internal error. Please repeat a request")
    }

//    private fun buildStartMessageCommand(chatId: Long, userGroups: List<UserGroupsResp>): BotApiMethod<Message> {
//        if (userGroups.isEmpty()) {
//            return SendMessage()
//                    .setChatId(chatId)
//                    .setText("Doesn't find one class group in the account")
//        }
//
//        val message = SendMessage().setChatId(chatId)
//        message.text = "select set for statistic"
//        val keyboardMarkup = ReplyKeyboardMarkup()
//        val rows = ArrayList<KeyboardRow>()
//
//        val statCommand = StatisticCommand(chatId, userGroups)
//        if (userGroups.size == 1) {
//            for (set in userGroups[0].sets) {
//                val row = KeyboardRow()
//                row.add(set.title)
//                rows.add(row)
//            }
//            // Set the keyboard to the markup
//            keyboardMarkup.keyboard = rows
//            // Add it to the message
//            message.replyMarkup = keyboardMarkup
//
//            statCommand.groupId = userGroups[0].id
//        } else {
//            for (group in userGroups) {
//                val row = KeyboardRow()
//                row.add(group.name)
//                rows.add(row)
//            }
//            // Set the keyboard to the markup
//            keyboardMarkup.keyboard = rows
//            // Add it to the message
//            message.replyMarkup = keyboardMarkup
//        }
//
//        commandQueue[chatId] = statCommand
//
//        return message
//    }

//    private fun defaultMessage(update: Update): BotApiMethod<Message> {
//        return if (usersService.getUser(update.message.chatId.toString())) {
//            sendCustomKeyboard(update)
//        } else {
//            usersService.create(update.message.chatId.toString())
//            SendMessage()
//                    .setChatId(update.message?.chatId)
//                    .setText("Need to connect quizlet.com account. Please use /connect command")
//        }
//    }

    private fun keyboard(update: Update): BotApiMethod<Message> {
        val message = SendMessage() // Create a message object object
                .setChatId(update.message.chatId)
                .setText("You send /start")

        val markupInline = InlineKeyboardMarkup()

        val rowsInline = ArrayList<List<InlineKeyboardButton>>()

        val rowInline = ArrayList<InlineKeyboardButton>()
        rowInline.add(InlineKeyboardButton().setText("Update message text").setCallbackData("update_msg_text"))

        // Set the keyboard to the markup
        rowsInline.add(rowInline)

        // Add it to the message
        markupInline.keyboard = rowsInline
        message.replyMarkup = markupInline

        return message
    }

    fun sendCustomKeyboard(update: Update): BotApiMethod<Message> {
        val message = SendMessage()
        message.chatId = update.message.chatId.toString()
        message.text = "To send text messages, please use the keyboard provided or the commands /start and /help."

        // Create ReplyKeyboardMarkup object
        val keyboardMarkup = ReplyKeyboardMarkup()

        // Create the keyboard (list of keyboard rows)
        val rows = ArrayList<KeyboardRow>()

        // Set each button, you can also use KeyboardButton objects if you need something else than text
        val statistics = KeyboardRow()
        statistics.add("Statistics")

        val notifications = KeyboardRow()
        notifications.add("Notifications")

        val account = KeyboardRow()
        account.add("Account")

        rows.add(statistics)
        rows.add(notifications)
        rows.add(account)

        // Set the keyboard to the markup
        keyboardMarkup.keyboard = rows
        // Add it to the message
        message.replyMarkup = keyboardMarkup

        return message
    }

    fun <T : Serializable> sendMessage(message: BotApiMethod<T>) {
        try {
            execute(message)
        } catch (e: TelegramApiException) {
            LOG.error("send message", e)
        }

    }

    fun <T : Serializable> sendMessage(message: BotApiMethod<T>, callback: SentCallback<T>) {
        try {
            executeAsync(message, callback)
        } catch (e: TelegramApiException) {
            LOG.error("async send message")
        }

    }

    fun sendAuthConfirmationMessage(chatId: String, login: String) {
        sendMessage(SendMessage()
                .setChatId(chatId)
                .setText("Account quizlet.com for user $login added"))
    }

}
