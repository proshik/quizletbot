package ru.proshik.english.quizlet.telegramBot.service

import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.proshik.english.quizlet.telegramBot.service.vo.NavigateType.*
import java.io.Serializable

class MessageBuilder {

    companion object {

        const val ALL_ITEMS = "-1"
    }

    /**
     * Build message with inline keyboard, where allow:
     *  - chose items;
     *  - update a keyboard with items to select another ones.
     *
     *  Example:
     *  ___________________________
     * | Message                   |
     * |                           |
     * | bla-bla-bla               |
     * |___________________________|
     * |___________ All ___________|
     * |_________ Item 1 __________| -> items, you can chose them
     * |_________ Item 2 __________|
     * |_ Prev 2 _ .3-4. _ Next 2 _| -> buttons that will update above items (Item 1, Item 2)
     *
     */
    fun buildStepPageKeyboardMessage(chatId: Long,
                                     messageText: String,
                                     items: List<Pair<String, String>>,
                                     messageId: Int? = null,
                                     itemInRow: Int = 1,
                                     firstElemInGroup: Int = 1,
                                     pagingButton: Boolean = false,
                                     showAllLine: Boolean = false): BotApiMethod<out Serializable> {

        // check input params
        if (items.isEmpty()) throw IllegalArgumentException("unexpectable count of values items=${items.size}")
        if (itemInRow < 1) throw IllegalArgumentException("unexpectable value itemInRow=$itemInRow")
        if (itemInRow > 1 && items.size < itemInRow)
            throw IllegalArgumentException("unexpectable value itemInRow=$itemInRow when size of items is ${items.size}")

        val keyboard = if (items.size > 1) buildInlineKeyboard(showAllLine, items, firstElemInGroup, itemInRow) else null

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

    /**
     * Build message with inline keyboard with paging by items
     *
     *  Example:
     *  __________________________________________
     * | Message 1                                |
     * |                                          | -> body message
     * | bla-bla-bla                              |
     * |__________________________________________|
     * |____.1.____ 2 ____ 3 ____ 4 _____ 19>>____| -> it is a paging
     *
     */
    fun buildItemPageKeyboardMessage(chatId: Long,
                                     messageId: Int,
                                     text: String,
                                     countOfItems: Int,
                                     selectedItem: Int = 1): BotApiMethod<out Serializable> {

        val keyboard = if (countOfItems > 1) buildInlineKeyboard(countOfItems, selectedItem) else null

        return EditMessageText()
                .setChatId(chatId)
                .setMessageId(messageId)
                .setText(text)
                .setReplyMarkup(keyboard)
                .enableMarkdown(true)
    }

    private fun buildInlineKeyboard(showAllLine: Boolean,
                                    items: List<Pair<String, String>>,
                                    firstElemInGroup: Int,
                                    itemInRow: Int): InlineKeyboardMarkup {
        val keyboard = InlineKeyboardMarkup()

        // initialize rows
        val rows = ArrayList<List<InlineKeyboardButton>>()
        // check and add the first line with a title "All items"
        if (showAllLine && items.size > 1) {
            val allElementRow = ArrayList<InlineKeyboardButton>()
            allElementRow.add(InlineKeyboardButton().setText("All items").setCallbackData("$NEXT_STEP;$ALL_ITEMS"))
            rows.add(allElementRow)
        }

        // main block. Fill items
        var i = 1
        var itemRows = ArrayList<InlineKeyboardButton>()
        val iterableItems = if (items.size > 4 && firstElemInGroup + 4 > items.size)
            items.subList(firstElemInGroup - 1, items.size)
        else if (items.size > 4) {
            items.subList(firstElemInGroup - 1, firstElemInGroup + 3)
        } else items
        for (item in iterableItems) {
            itemRows.add(InlineKeyboardButton().setText(item.first).setCallbackData("$NEXT_STEP;${item.second}"))

            if (i % itemInRow == 0) {
                rows.add(itemRows)
                i = 1
                itemRows = ArrayList()
            } else {
                i++
            }
        }
        rows.add(itemRows)

        // add buttons to execute
        val navigationRow = ArrayList<InlineKeyboardButton>()
        if (items.size in 5..8) {
            if (firstElemInGroup < 5) {
                navigationRow.add(InlineKeyboardButton().setText("·1-4·").setCallbackData("$PAGING_BY_BUTTON;1"))
                navigationRow.add(InlineKeyboardButton().setText("5-${items.size}").setCallbackData("$PAGING_BY_BUTTON;5"))
            } else {
                navigationRow.add(InlineKeyboardButton().setText("1-4").setCallbackData("$PAGING_BY_BUTTON;1"))
                navigationRow.add(InlineKeyboardButton().setText("·5-${items.size}·").setCallbackData("$PAGING_BY_BUTTON;5"))
            }
        } else if (items.size > 8) {
            when (firstElemInGroup) {
                1 -> {
                    navigationRow.add(InlineKeyboardButton().setText("·1-4·").setCallbackData("$PAGING_BY_BUTTON;1"))
                    navigationRow.add(InlineKeyboardButton().setText("Next").setCallbackData("$PAGING_BY_BUTTON;5"))
                }
                in items.size - 4..items.size -> {
                    navigationRow.add(InlineKeyboardButton().setText("Prev").setCallbackData("$PAGING_BY_BUTTON;${firstElemInGroup - 4}"))
                    val text = if (firstElemInGroup == items.size) {
                        "$firstElemInGroup"
                    } else {
                        "$firstElemInGroup-${items.size}"
                    }
                    navigationRow.add(InlineKeyboardButton().setText("·$text·").setCallbackData("$PAGING_BY_BUTTON;$firstElemInGroup"))
                }
                else -> {
                    navigationRow.add(InlineKeyboardButton().setText("Prev").setCallbackData("$PAGING_BY_BUTTON;${firstElemInGroup - 4}"))
                    navigationRow.add(InlineKeyboardButton().setText("·$firstElemInGroup-${firstElemInGroup + 3}·").setCallbackData("$PAGING_BY_BUTTON;$firstElemInGroup"))
                    navigationRow.add(InlineKeyboardButton().setText("Next").setCallbackData("$PAGING_BY_BUTTON;${firstElemInGroup + 4}"))
                }
            }
        }
        rows.add(navigationRow)

        keyboard.keyboard = rows
        return keyboard
    }

    private fun buildInlineKeyboard(countOfItems: Int, selectedItem: Int): InlineKeyboardMarkup {
        val keyboard = InlineKeyboardMarkup()

        // initialize rows
        val rows = ArrayList<List<InlineKeyboardButton>>()

        // build keyboards
        val numberRow = ArrayList<InlineKeyboardButton>()

        if (countOfItems > 5) {
            if (selectedItem > 3 && selectedItem < (countOfItems - 2)) {
                numberRow.add(InlineKeyboardButton().setText("<<1").setCallbackData("$PAGING_BY_ITEM;1"))
                numberRow.add(InlineKeyboardButton().setText("${selectedItem - 1}").setCallbackData("$PAGING_BY_ITEM;${selectedItem - 1}"))
                numberRow.add(InlineKeyboardButton().setText("·$selectedItem·").setCallbackData("$PAGING_BY_ITEM;$selectedItem"))
                numberRow.add(InlineKeyboardButton().setText("${selectedItem + 1}").setCallbackData("$PAGING_BY_ITEM;${selectedItem + 1}"))
                numberRow.add(InlineKeyboardButton().setText("$countOfItems>>").setCallbackData("$PAGING_BY_ITEM;$countOfItems"))
            } else {
                if (selectedItem < 4) {
                    if (selectedItem == 1) {
                        numberRow.add(InlineKeyboardButton().setText("·1·").setCallbackData("$PAGING_BY_ITEM;1"))
                    } else {
                        numberRow.add(InlineKeyboardButton().setText("1").setCallbackData("$PAGING_BY_ITEM;1"))
                    }
                    for (item in 2..4) {
                        if (selectedItem == item) {
                            numberRow.add(InlineKeyboardButton().setText("·$item·").setCallbackData("$PAGING_BY_ITEM;$item"))
                        } else {
                            numberRow.add(InlineKeyboardButton().setText("$item").setCallbackData("$PAGING_BY_ITEM;$item"))
                        }
                    }
                    numberRow.add(InlineKeyboardButton().setText("$countOfItems>>").setCallbackData("$PAGING_BY_ITEM;$countOfItems"))
                } else {
                    numberRow.add(InlineKeyboardButton().setText("<<1").setCallbackData("$PAGING_BY_ITEM;1"))
                    for (item in countOfItems - 3 until countOfItems) {
                        if (selectedItem == item) {
                            numberRow.add(InlineKeyboardButton().setText("·$item·").setCallbackData("$PAGING_BY_ITEM;$item"))
                        } else {
                            numberRow.add(InlineKeyboardButton().setText("$item").setCallbackData("$PAGING_BY_ITEM;$item"))
                        }
                    }
                    if (selectedItem == countOfItems) {
                        numberRow.add(InlineKeyboardButton().setText("·$countOfItems·").setCallbackData("$PAGING_BY_ITEM;$countOfItems"))
                    } else {
                        numberRow.add(InlineKeyboardButton().setText("$countOfItems").setCallbackData("$PAGING_BY_ITEM;$countOfItems"))
                    }
                }
            }
        } else {
            for (item in 1..countOfItems) {
                val title = if (selectedItem == item) "·$item·" else "$item"
                numberRow.add(InlineKeyboardButton().setText(title).setCallbackData("$PAGING_BY_ITEM;$item"))
            }
        }

        rows.add(numberRow)

        keyboard.keyboard = rows
        return keyboard
    }

}
