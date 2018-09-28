package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import ru.proshik.english.quizlet.telegramBot.service.model.CommandType
import ru.proshik.english.quizlet.telegramBot.service.operation.BotOperationService
import java.io.Serializable

@Service
class BotCommandService(private val authenticationService: AuthenticationService) {

    companion object {
        private const val DEFAULT_MESSAGE = "This bot can help you get information about studied sets on quizlet.com"
    }

    fun handleCommand(chatId: Long, text: String): BotApiMethod<out Serializable> {
        val commandType = CommandType.getByName(text) ?: return SendMessage().setChatId(chatId).setText(
                """Command doesn't exist.
                        |Please, use the keyboard""".trimMargin())

        return when (commandType) {
            CommandType.START -> SendMessage()
                    .setChatId(chatId)
                    .setText(DEFAULT_MESSAGE)
                    .setReplyMarkup(buildAuthorizeMenu())
            CommandType.HELP -> SendMessage()
                    .setChatId(chatId)
                    .setText(DEFAULT_MESSAGE)
            //TODO add more valid texts for messages
            CommandType.AUTHORIZE -> SendMessage()
                    .setChatId(chatId)
                    .setText(authenticationService.connectToQuizlet(chatId))
            CommandType.RE_AUTHORIZE -> SendMessage()
                    .setChatId(chatId)
                    .setText(authenticationService.connectToQuizlet(chatId))
        }
    }

    private fun buildAuthorizeMenu(): ReplyKeyboardMarkup {
        val keyboardMarkup = ReplyKeyboardMarkup().apply {
            resizeKeyboard = true
            selective = true
        }

        val rows = ArrayList<KeyboardRow>()
        val row = KeyboardRow()
        row.add(BotOperationService.GreetingsMenu.AUTHORIZE.title)
        rows.add(row)

        keyboardMarkup.keyboard = rows

        return keyboardMarkup
    }

    private fun buildMainMenu(): ReplyKeyboardMarkup {
        val keyboardMarkup = ReplyKeyboardMarkup().apply {
            resizeKeyboard = true
            selective = true
        }

        val rows = ArrayList<KeyboardRow>()
        for (operation in BotOperationService.OPERATIONS) {
            val row = KeyboardRow()
            row.add(operation)
            rows.add(row)
        }

        keyboardMarkup.keyboard = rows

        return keyboardMarkup
    }

}