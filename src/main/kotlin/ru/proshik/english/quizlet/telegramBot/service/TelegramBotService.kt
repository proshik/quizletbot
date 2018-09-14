package ru.proshik.english.quizlet.telegramBot.service

import org.apache.log4j.Logger
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
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


@Service
class TelegramBotService(@Lazy private val bot: TelegramBot) {

    companion object {

        private val LOG = Logger.getLogger(TelegramBotService::class.java)
    }

    fun onWebHookUpdateReceived(update: Update): BotApiMethod<Message> = when {
        update.hasMessage() -> message(update)
        update.hasCallbackQuery() -> callback(update)
        else -> throw RuntimeException("Unexpected situation")
    }

    private fun message(update: Update): BotApiMethod<Message> = when {
        update.message.isCommand -> commandMessage(update)
        isOperationMessage(update) -> operationMessage(update)
        else -> defaultMessage(update)
    }

    private fun callback(update: Update): BotApiMethod<Message> {
        return SendMessage().setChatId(update.callbackQuery.message.chatId).setText("Callback message")
    }

    private fun commandMessage(update: Update): BotApiMethod<Message> {
        return when (update.message.text.split(" ")[0]) {
            "/start" -> SendMessage().setChatId(update.message?.chatId).setText("Start message")
            "/help" -> SendMessage().setChatId(update.message?.chatId).setText("Help message")
            "/inlinekeyboard" -> keyboard(update)
            "/replaykeyboard" -> sendCustomKeyboard(update)
            else -> throw RuntimeException("Unexpected command")
        }
    }

    private fun isOperationMessage(update: Update): Boolean {
        return false
    }

    private fun operationMessage(update: Update): BotApiMethod<Message> {
        return SendMessage().setChatId(update.message?.chatId).setText("Command message")
    }

    private fun defaultMessage(update: Update): BotApiMethod<Message> {
        return SendMessage()
                .setChatId(update.message?.chatId)
                .setText("Default message")
    }

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
        message.text = "Custom message text"

        // Create ReplyKeyboardMarkup object
        val keyboardMarkup = ReplyKeyboardMarkup()
        // Create the keyboard (list of keyboard rows)
        val keyboard = ArrayList<KeyboardRow>()
        // Create a keyboard row
        var row = KeyboardRow()
        // Set each button, you can also use KeyboardButton objects if you need something else than text
        row.add("Row 1 Button 1")
        row.add("Row 1 Button 2")
        row.add("Row 1 Button 3")
        // Add the first row to the keyboard
        keyboard.add(row)
        // Create another keyboard row
        row = KeyboardRow()
        // Set each button for the second line
        row.add("Row 2 Button 1")
        row.add("Row 2 Button 2")
        row.add("Row 2 Button 3")
        // Add the second row to the keyboard
        keyboard.add(row)
        // Set the keyboard to the markup
        keyboardMarkup.keyboard = keyboard
        // Add it to the message
        message.replyMarkup = keyboardMarkup

        return message
    }

    fun <T : Serializable> sendMessage(message: BotApiMethod<T>) {
        try {
            bot.execute(message)
        } catch (e: TelegramApiException) {
            LOG.error("send message", e)
        }

    }

    fun <T : Serializable> sendMessage(message: BotApiMethod<T>, callback: SentCallback<T>) {
        try {
            bot.executeAsync(message, callback)
        } catch (e: TelegramApiException) {
            LOG.error("async send message")
        }

    }

    fun sendAuthConfirmationMessage(chatId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
