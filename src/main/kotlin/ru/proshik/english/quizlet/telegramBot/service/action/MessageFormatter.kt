package ru.proshik.english.quizlet.telegramBot.service.action

import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.io.Serializable

class MessageFormatter {

    fun messageInlineKeyboard(chatId: Long, data: InlineKeyboardData): BotApiMethod<out Serializable> {
        val markupInline = buildInlineKeyboardMarkup(data)

        return SendMessage().setChatId(chatId).setText(data.text).setReplyMarkup(markupInline)
    }

    fun editMessageInlineKeyboard(chatId: Long, data: InlineKeyboardData): BotApiMethod<out Serializable> {
        if (data.messageId == null) throw RuntimeException("messageId not must be null")

        val markupInline = buildInlineKeyboardMarkup(data)

        return EditMessageReplyMarkup().setChatId(chatId).setReplyMarkup(markupInline).setMessageId(data.messageId)
    }

    fun editMessageReplyMarkup(chatId: Long, data: InlineKeyboardData): BotApiMethod<out Serializable> {
        if (data.messageId == null) throw RuntimeException("messageId not must be null")

        val markupInline = buildInlineKeyboardMarkup(data)

        return EditMessageReplyMarkup().setChatId(chatId).setMessageId(data.messageId).setReplyMarkup(markupInline)
    }

    fun removeEditMessageReplyMarkup(chatId: Long, data: InlineKeyboardData): BotApiMethod<out Serializable> {
        return EditMessageReplyMarkup().setChatId(chatId).setMessageId(data.messageId).setReplyMarkup(null)
    }


    private fun buildInlineKeyboardMarkup(data: InlineKeyboardData): InlineKeyboardMarkup {
        val markupInline = InlineKeyboardMarkup()

        val rows = ArrayList<List<InlineKeyboardButton>>()
        // Set the keyboard to the markup
        //        if (all) {
        //            val rowInline = ArrayList<InlineKeyboardButton>()
        //            rowInline.add(InlineKeyboardButton().setText("All").setCallbackData("All"))
        //            rows.add(rowInline)
        //        }

        var i = 1
        var row = ArrayList<InlineKeyboardButton>()
        for (item in data.items) {

            row.add(InlineKeyboardButton().setText(item.first).setCallbackData(item.second))

            if (i % 4 == 0) {
                rows.add(row)
                i = 1
                row = ArrayList()
            } else {
                i++
            }
        }

        rows.add(row)

        markupInline.keyboard = rows
        return markupInline
    }

}