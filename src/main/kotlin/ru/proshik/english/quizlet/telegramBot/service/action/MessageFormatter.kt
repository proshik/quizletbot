package ru.proshik.english.quizlet.telegramBot.service.action

import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.io.Serializable

class MessageFormatter {

    companion object {
        const val NAVIGATION = "nav"
        const val ELEMENT = "elem"

        const val ALL_ITEMS = "-1"
    }

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
        val keyboard = InlineKeyboardMarkup()

        val rows = ArrayList<List<InlineKeyboardButton>>()

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

        keyboard.keyboard = rows
        return keyboard
    }


    fun navigateBySteps(chatId: Long,
                        messageText: String,
                        items: List<Pair<String, String>>,
                        messageId: Int? = null,
                        itemInRow: Int = 1,
                        firstElementOfGroup: Int = 0,
                        showAllLine: Boolean = false): BotApiMethod<out Serializable> {

        // check input params
        if (items.isEmpty()) throw IllegalArgumentException("unexpectable count of values items=${items.size}")
        if (itemInRow < 1) throw IllegalArgumentException("unexpectable value itemInRow=$itemInRow")

        // calculate value of items on one line
        //TODO fix bug
        val countItemOnLine = 1
//                if (itemInRow != 1 && items.size > itemInRow) itemInRow else items.size
        // initialize rows
        val rows = ArrayList<List<InlineKeyboardButton>>()
        // check and add the first line with a title "All items"
        if (showAllLine && items.size > 1) {
            val allElementRow = ArrayList<InlineKeyboardButton>()
            allElementRow.add(InlineKeyboardButton().setText("All items").setCallbackData("$ELEMENT;$ALL_ITEMS"))
            rows.add(allElementRow)
        }

        // main block. Fill items
        var i = 1
        var itemRows = ArrayList<InlineKeyboardButton>()
        for (item in items) {
            itemRows.add(InlineKeyboardButton().setText(item.first).setCallbackData("$ELEMENT;${item.second}"))

            if (i % countItemOnLine == 0) {
                rows.add(itemRows)
                i = 1
                itemRows = ArrayList()
            } else {
                i++
            }
        }
        rows.add(itemRows)

        // add buttons to navigate
//        if (items.size > 8) {
//            InlineKeyboardButton().setText(item.first).setCallbackData(item.second)
//            InlineKeyboardButton().setText(item.first).setCallbackData(item.second)
//            InlineKeyboardButton().setText(item.first).setCallbackData(item.second)
//        } else if (items.size > 4) {
//
//        }

        val keyboard = InlineKeyboardMarkup()
        keyboard.keyboard = rows

        return if (messageId == null)
            SendMessage()
                    .setChatId(chatId)
                    .setText(messageText)
                    .enableMarkdown(true)
                    .setReplyMarkup(keyboard)
        else
            EditMessageText()
                    .setChatId(chatId)
                    .setMessageId(messageId)
                    .setText(messageText)
                    .enableMarkdown(true)
                    .setReplyMarkup(keyboard)
    }

    fun navigateByItems(chatId: Long,
                        messageId: Int,
                        text: String,
                        countOfItems: Int,
                        selectedItem: Int = 1): BotApiMethod<out Serializable> {

        // check input params
//        if (countOfItems) throw IllegalArgumentException("unexpectable count of values items=${countOfItems}")

        // initialize rows
        val rows = ArrayList<List<InlineKeyboardButton>>()

        // build keyboards
        val numberRow = ArrayList<InlineKeyboardButton>()
        if (countOfItems > 5) {
            if (selectedItem > 2 && selectedItem < (countOfItems - 2)) {
                numberRow.add(InlineKeyboardButton().setText("<<1").setCallbackData(1.toString()))
                numberRow.add(InlineKeyboardButton().setText("${selectedItem - 1}").setCallbackData("$NAVIGATION;${selectedItem - 1}"))
                numberRow.add(InlineKeyboardButton().setText(".$selectedItem.").setCallbackData("$NAVIGATION;$selectedItem"))
                numberRow.add(InlineKeyboardButton().setText("${selectedItem + 1}").setCallbackData("$NAVIGATION;${selectedItem + 1}"))
                numberRow.add(InlineKeyboardButton().setText("$countOfItems>>").setCallbackData(countOfItems.toString()))
            } else {
                numberRow.add(InlineKeyboardButton().setText("1").setCallbackData(1.toString()))
                for (item in selectedItem - 1..selectedItem + 1) {
                    val title = if (item == selectedItem) ".$item." else item.toString()
                    numberRow.add(InlineKeyboardButton().setText(title).setCallbackData("$NAVIGATION;$item"))
                }
                numberRow.add(InlineKeyboardButton().setText("${countOfItems}>>").setCallbackData("$NAVIGATION;${countOfItems}"))
            }
        } else {
            for (item in 1..countOfItems) {
                val title = if (selectedItem == item) ".$item." else "$item"
                numberRow.add(InlineKeyboardButton().setText(title).setCallbackData("$NAVIGATION;$item"))
            }
        }

        rows.add(numberRow)

        val keyboard = InlineKeyboardMarkup()
        keyboard.keyboard = rows

        return EditMessageText()
                .setChatId(chatId)
                .setMessageId(messageId)
                .setText(text)
                .setReplyMarkup(keyboard)
                .enableMarkdown(true)
    }

    //TODO old implementation
    fun formatStep(chatId: Long, items: List<String>,
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


}