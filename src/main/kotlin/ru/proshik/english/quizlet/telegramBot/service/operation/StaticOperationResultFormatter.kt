package ru.proshik.english.quizlet.telegramBot.service.operation

import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow

class StaticOperationResultFormatter(val chatId: String) : OperationResultFormatter<StaticOperationResult> {

    override fun format(result: StaticOperationResult): BotApiMethod<Message> {
        if (result.isTerminate) {
            val statistics = result.done()

            return SendMessage()
                    .setChatId(chatId)
                    .setText("Result for group ${statistics.groupId}")
        } else {
            val message = SendMessage().setChatId(chatId)

            val stepInfo = result.step()

            val keyboardMarkup = ReplyKeyboardMarkup()
            val rows = ArrayList<KeyboardRow>()
            for (item in stepInfo) {
                val row = KeyboardRow()
                row.add(item)
                rows.add(row)
            }

            keyboardMarkup.keyboard = rows

            message.replyMarkup = keyboardMarkup

            return message
        }
    }

}
