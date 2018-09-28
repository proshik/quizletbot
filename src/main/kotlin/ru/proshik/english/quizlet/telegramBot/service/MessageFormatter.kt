package ru.proshik.english.quizlet.telegramBot.service

import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.io.Serializable

class MessageFormatter {

    // todo replace constants to enums
    companion object {
        const val PAGING_ELEMENT = "page_elem"
        const val PAGING_BUTTONS = "page_butt"
        const val STEPPING = "step"

        const val ALL_ITEMS = "-1"
    }

    fun navigateBySteps(chatId: Long,
                        messageText: String,
                        items: List<Pair<String, String>>,
                        messageId: Int? = null,
                        itemInRow: Int = 1,
                        firstElemInGr: Int = 1,
                        pagingButton: Boolean = false,
                        showAllLine: Boolean = false): BotApiMethod<out Serializable> {

        // check input params
        if (items.isEmpty()) throw IllegalArgumentException("unexpectable count of values items=${items.size}")
        if (itemInRow < 1) throw IllegalArgumentException("unexpectable value itemInRow=$itemInRow")
        if (itemInRow > 1 && items.size < itemInRow)
            throw IllegalArgumentException("unexpectable value itemInRow=$itemInRow when size of items is ${items.size}")

        // initialize rows
        val rows = ArrayList<List<InlineKeyboardButton>>()
        // check and add the first line with a title "All items"
        if (showAllLine && items.size > 1) {
            val allElementRow = ArrayList<InlineKeyboardButton>()
            allElementRow.add(InlineKeyboardButton().setText("All items").setCallbackData("$STEPPING;$ALL_ITEMS"))
            rows.add(allElementRow)
        }

        // main block. Fill items
        var i = 1
        var itemRows = ArrayList<InlineKeyboardButton>()
        val iterableItems = if (items.size > 4 && firstElemInGr + 4 > items.size)
            items.subList(firstElemInGr - 1, items.size)
        else if (items.size > 4) {
            items.subList(firstElemInGr - 1, firstElemInGr + 3)
        } else items
        for (item in iterableItems) {
            itemRows.add(InlineKeyboardButton().setText(item.first).setCallbackData("$STEPPING;${item.second}"))

            if (i % itemInRow == 0) {
                rows.add(itemRows)
                i = 1
                itemRows = ArrayList()
            } else {
                i++
            }
        }
        rows.add(itemRows)

        // add buttons to navigate
        val navigationRow = ArrayList<InlineKeyboardButton>()
        if (items.size in 5..8) {
            if (firstElemInGr < 5) {
                navigationRow.add(InlineKeyboardButton().setText("·1-4·").setCallbackData("$PAGING_BUTTONS;1"))
                navigationRow.add(InlineKeyboardButton().setText("5-${items.size}").setCallbackData("$PAGING_BUTTONS;5"))
            } else {
                navigationRow.add(InlineKeyboardButton().setText("1-4").setCallbackData("$PAGING_BUTTONS;1"))
                navigationRow.add(InlineKeyboardButton().setText("·5-${items.size}·").setCallbackData("$PAGING_BUTTONS;5"))
            }
        } else if (items.size > 8) {
            if (firstElemInGr == 1) {
                navigationRow.add(InlineKeyboardButton().setText("·1-4·").setCallbackData("$PAGING_BUTTONS;1"))
                navigationRow.add(InlineKeyboardButton().setText("Next").setCallbackData("$PAGING_BUTTONS;5"))
            } else if (firstElemInGr in items.size - 4..items.size) {
                navigationRow.add(InlineKeyboardButton().setText("Prev").setCallbackData("$PAGING_BUTTONS;${firstElemInGr - 4}"))
                val text = if (firstElemInGr == items.size) {
                    "$firstElemInGr"
                } else {
                    "$firstElemInGr-${items.size}"
                }
                navigationRow.add(InlineKeyboardButton().setText("·$text·").setCallbackData("$PAGING_BUTTONS;$firstElemInGr"))
            } else {
                navigationRow.add(InlineKeyboardButton().setText("Prev").setCallbackData("$PAGING_BUTTONS;${firstElemInGr - 4}"))
                navigationRow.add(InlineKeyboardButton().setText("·$firstElemInGr-${firstElemInGr + 3}·").setCallbackData("$PAGING_BUTTONS;$firstElemInGr"))
                navigationRow.add(InlineKeyboardButton().setText("Next").setCallbackData("$PAGING_BUTTONS;${firstElemInGr + 4}"))
            }
        }
        rows.add(navigationRow)

        val keyboard = InlineKeyboardMarkup()
        keyboard.keyboard = rows

        if (pagingButton) {
            return EditMessageReplyMarkup()
                    .setChatId(chatId)
                    .setMessageId(messageId)
                    .setReplyMarkup(keyboard)
        }

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


        val keyboard = if (countOfItems > 1) buildIInlinePagingKeyboard(countOfItems, selectedItem) else null

        return EditMessageText()
                .setChatId(chatId)
                .setMessageId(messageId)
                .setText(text)
                .setReplyMarkup(keyboard)
                .enableMarkdown(true)
    }

    private fun buildIInlinePagingKeyboard(countOfItems: Int, selectedItem: Int): InlineKeyboardMarkup {
        val keyboard = InlineKeyboardMarkup()

        // initialize rows
        val rows = ArrayList<List<InlineKeyboardButton>>()

        // build keyboards
        val numberRow = ArrayList<InlineKeyboardButton>()

        if (countOfItems > 5) {
            if (selectedItem > 3 && selectedItem < (countOfItems - 2)) {
                numberRow.add(InlineKeyboardButton().setText("<<1").setCallbackData("$PAGING_ELEMENT;1"))
                numberRow.add(InlineKeyboardButton().setText("${selectedItem - 1}").setCallbackData("$PAGING_ELEMENT;${selectedItem - 1}"))
                numberRow.add(InlineKeyboardButton().setText("·$selectedItem·").setCallbackData("$PAGING_ELEMENT;$selectedItem"))
                numberRow.add(InlineKeyboardButton().setText("${selectedItem + 1}").setCallbackData("$PAGING_ELEMENT;${selectedItem + 1}"))
                numberRow.add(InlineKeyboardButton().setText("$countOfItems>>").setCallbackData("$PAGING_ELEMENT;$countOfItems"))
            } else {
                if (selectedItem < 4) {
                    if (selectedItem == 1) {
                        numberRow.add(InlineKeyboardButton().setText("·1·").setCallbackData("$PAGING_ELEMENT;1"))
                    } else {
                        numberRow.add(InlineKeyboardButton().setText("1").setCallbackData("$PAGING_ELEMENT;1"))
                    }
                    for (item in 2..4) {
                        if (selectedItem == item) {
                            numberRow.add(InlineKeyboardButton().setText("·$item·").setCallbackData("$PAGING_ELEMENT;$item"))
                        } else {
                            numberRow.add(InlineKeyboardButton().setText("$item").setCallbackData("$PAGING_ELEMENT;$item"))
                        }
                    }
                    numberRow.add(InlineKeyboardButton().setText("$countOfItems>>").setCallbackData("$PAGING_ELEMENT;$countOfItems"))
                } else {
                    numberRow.add(InlineKeyboardButton().setText("<<1").setCallbackData("$PAGING_ELEMENT;1"))
                    for (item in countOfItems - 3 until countOfItems) {
                        if (selectedItem == item) {
                            numberRow.add(InlineKeyboardButton().setText("·$item·").setCallbackData("$PAGING_ELEMENT;$item"))
                        } else {
                            numberRow.add(InlineKeyboardButton().setText("$item").setCallbackData("$PAGING_ELEMENT;$item"))
                        }
                    }
                    if (selectedItem == countOfItems) {
                        numberRow.add(InlineKeyboardButton().setText("·$countOfItems·").setCallbackData("$PAGING_ELEMENT;$countOfItems"))
                    } else {
                        numberRow.add(InlineKeyboardButton().setText("$countOfItems").setCallbackData("$PAGING_ELEMENT;$countOfItems"))
                    }
                }
            }
        } else {
            for (item in 1..countOfItems) {
                val title = if (selectedItem == item) "·$item·" else "$item"
                numberRow.add(InlineKeyboardButton().setText(title).setCallbackData("$PAGING_ELEMENT;$item"))
            }
        }

        rows.add(numberRow)

        keyboard.keyboard = rows
        return keyboard
    }

}
