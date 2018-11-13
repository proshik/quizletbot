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

        const val ALL = "↔️"
        const val BACK = "⬅️"
        const val NEXT = "⏩"
        const val PREV = "⏪"
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
                                     prefix: String = "",
                                     messageId: Int? = null,
                                     itemInRow: Int = 1,
                                     firstElemInGroup: Int = 1,
                                     pagingButton: Boolean = false,
                                     showAllLine: Boolean = false,
                                     prevStep: Boolean = false): BotApiMethod<out Serializable> {

        // check input params
        if (items.isEmpty()) throw IllegalArgumentException("unexpectable count of values items=${items.size}")
        if (itemInRow < 1) throw IllegalArgumentException("unexpectable value itemInRow=$itemInRow")
        if (itemInRow > 1 && items.size < itemInRow)
            throw IllegalArgumentException("unexpectable value itemInRow=$itemInRow when size of items is ${items.size}")

        val keyboard = if (items.size > 1) buildInlineItemPageRow(showAllLine, items, prefix, firstElemInGroup,
                itemInRow, prevStep) else null

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


    private fun buildInlineItemPageRow(showAllLine: Boolean,
                                       items: List<Pair<String, String>>,
                                       prefix: String,
                                       firstElemInGroup: Int,
                                       itemInRow: Int,
                                       prevStep: Boolean): InlineKeyboardMarkup {
        val keyboard = InlineKeyboardMarkup()

        // initialize rows
        val rows = ArrayList<List<InlineKeyboardButton>>()
        // check and add the first line with a title "All items"
        if (showAllLine && items.size > 1) {
            val allElementRow = ArrayList<InlineKeyboardButton>()
            allElementRow.add(InlineKeyboardButton().setText("$ALL All items").setCallbackData("$prefix;$NEXT_STEP;$ALL_ITEMS"))
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
            itemRows.add(InlineKeyboardButton().setText(item.first).setCallbackData("$prefix;$NEXT_STEP;${item.second}"))

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
        val row = ArrayList<InlineKeyboardButton>()
        if (items.size in 5..8) {
            if (firstElemInGroup < 5) {
                row.add(InlineKeyboardButton().setText("·1-4·").setCallbackData("$prefix;$PAGING_BY_BUTTONS;1"))
                if (items.size == 5) {
                    row.add(InlineKeyboardButton().setText("5").setCallbackData("$prefix;$PAGING_BY_BUTTONS;5"))
                } else {
                    row.add(InlineKeyboardButton().setText("5-${items.size}").setCallbackData("$prefix;$PAGING_BY_BUTTONS;5"))
                }
            } else if (items.size == 5) {
                row.add(InlineKeyboardButton().setText("1-4").setCallbackData("$prefix;$PAGING_BY_BUTTONS;1"))
                row.add(InlineKeyboardButton().setText("·5·").setCallbackData("$prefix;$PAGING_BY_BUTTONS;5"))
            } else {
                row.add(InlineKeyboardButton().setText("1-4").setCallbackData("$prefix;$PAGING_BY_BUTTONS;1"))
                row.add(InlineKeyboardButton().setText("·5-${items.size}·").setCallbackData("$prefix;$PAGING_BY_BUTTONS;5"))
            }
        } else if (items.size > 8) {
            when (firstElemInGroup) {
                1 -> {
                    row.add(InlineKeyboardButton().setText("·1-4·").setCallbackData("$prefix;$PAGING_BY_BUTTONS;1"))
                    row.add(InlineKeyboardButton().setText("Next »").setCallbackData("$prefix;$PAGING_BY_BUTTONS;5"))
                }
                in items.size - 4..items.size -> {
                    row.add(InlineKeyboardButton().setText("« Prev").setCallbackData("$prefix;$PAGING_BY_BUTTONS;${firstElemInGroup - 4}"))
                    val text = if (firstElemInGroup == items.size) {
                        "$firstElemInGroup"
                    } else {
                        "$firstElemInGroup-${items.size}"
                    }
                    row.add(InlineKeyboardButton().setText("·$text·").setCallbackData("$prefix;$PAGING_BY_BUTTONS;$firstElemInGroup"))
                }
                else -> {
                    row.add(InlineKeyboardButton().setText("« Prev").setCallbackData("$prefix;$PAGING_BY_BUTTONS;${firstElemInGroup - 4}"))
                    row.add(InlineKeyboardButton().setText("·$firstElemInGroup-${firstElemInGroup + 3}·").setCallbackData("$prefix;$PAGING_BY_BUTTONS;$firstElemInGroup"))
                    row.add(InlineKeyboardButton().setText("Next »").setCallbackData("$prefix;$PAGING_BY_BUTTONS;${firstElemInGroup + 4}"))
                }
            }
        }
        rows.add(row)

        if (prevStep) {
            rows.add(buildBackRow(prefix))
        }

        keyboard.keyboard = rows
        return keyboard
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
     * With additional buttons:
     *
     *  __________________________________________
     * | Message 1                                |
     * |                                          | -> body message
     * | bla-bla-bla                              |
     * |__________________________________________|
     * |____.1.____ 2 ____ 3 ____ 4 _____ 19>>____| -> it is a paging
     * |___Button1___|___Button2___|___Button3____| -> it is extraButtons
     */
    fun buildItemPageKeyboardMessage(chatId: Long,
                                     messageId: Int,
                                     text: String,
                                     itemIds: List<Long>,
                                     prefix: String,
                                     selectedItem: Int = 1,
                                     prevStep: Boolean = false,
                                     extraButtons: List<Pair<String, String>> = emptyList()): BotApiMethod<out Serializable> {

        val keyboard = InlineKeyboardMarkup()
        // initialize rows
        val rows = ArrayList<List<InlineKeyboardButton>>()

        if (itemIds.size > 1) {
            val navigateRow = buildInlineItemPageRow(itemIds, selectedItem, prefix)
            rows.add(navigateRow)
        }
        if (extraButtons.isNotEmpty()) {
            val additionalRow = ArrayList<InlineKeyboardButton>()
            for (item in extraButtons) {
                additionalRow.add(InlineKeyboardButton().setText(item.first).setCallbackData("$prefix;$UPDATE_ITEMS;${item.second}"))
            }
            rows.add(additionalRow)
        }

        if (prevStep) {
            rows.add(buildBackRow(prefix))
        }

        keyboard.keyboard = rows

        return EditMessageText()
                .setChatId(chatId)
                .setMessageId(messageId)
                .setText(text)
                .setReplyMarkup(keyboard)
                .enableMarkdown(true)
    }

    private fun buildInlineItemPageRow(itemIds: List<Long>,
                                       selectedItem: Int,
                                       prefix: String): List<InlineKeyboardButton> {

        // build keyboards
        val row = ArrayList<InlineKeyboardButton>()

        if (itemIds.size > 5) {
            if (selectedItem > 3 && selectedItem < (itemIds.size - 2)) {
                row.add(InlineKeyboardButton().setText("« 1").setCallbackData("$prefix;$PAGING_BY_ITEMS;${itemIds[0]}"))
                row.add(InlineKeyboardButton().setText("${selectedItem - 1}").setCallbackData("$prefix;$PAGING_BY_ITEMS;${itemIds[selectedItem - 2]}"))
                row.add(InlineKeyboardButton().setText("·$selectedItem·").setCallbackData("$prefix;$PAGING_BY_ITEMS;${itemIds[selectedItem]}"))
                    row.add(InlineKeyboardButton().setText("${selectedItem + 1}").setCallbackData("$prefix;$PAGING_BY_ITEMS;${itemIds[selectedItem]}"))
                row.add(InlineKeyboardButton().setText("${itemIds.size} »").setCallbackData("$prefix;$PAGING_BY_ITEMS;${itemIds.last()}"))
            } else {
                if (selectedItem < 4) {
                    if (selectedItem == 1) {
                        row.add(InlineKeyboardButton().setText("·1·").setCallbackData("$prefix;$PAGING_BY_ITEMS;${itemIds[0]}"))
                    } else {
                        row.add(InlineKeyboardButton().setText("1").setCallbackData("$prefix;$PAGING_BY_ITEMS;${itemIds[0]}"))
                    }
                    for (item in 2..4) {
                        if (selectedItem == item) {
                            row.add(InlineKeyboardButton().setText("·$item·").setCallbackData("$prefix;$PAGING_BY_ITEMS;${itemIds[item - 1]}"))
                        } else {
                            row.add(InlineKeyboardButton().setText("$item").setCallbackData("$prefix;$PAGING_BY_ITEMS;${itemIds[item - 1]}"))
                        }
                    }
                    row.add(InlineKeyboardButton().setText("${itemIds.size} »").setCallbackData("$prefix;$PAGING_BY_ITEMS;${itemIds.last()}"))
                } else {
                    row.add(InlineKeyboardButton().setText("« 1").setCallbackData("$prefix;$PAGING_BY_ITEMS;${itemIds[0]}"))
                    for (item in itemIds.size - 3 until itemIds.size) {
                        if (selectedItem == item) {
                            row.add(InlineKeyboardButton().setText("·$item·").setCallbackData("$prefix;$PAGING_BY_ITEMS;${itemIds[item - 1]}"))
                        } else {
                            row.add(InlineKeyboardButton().setText("$item").setCallbackData("$prefix;$PAGING_BY_ITEMS;${itemIds[item - 1]}"))
                        }
                    }
                    if (selectedItem == itemIds.size) {
                        row.add(InlineKeyboardButton().setText("·${itemIds.size}·").setCallbackData("$prefix;$PAGING_BY_ITEMS;${itemIds.last()}"))
                    } else {
                        row.add(InlineKeyboardButton().setText("${itemIds.size}").setCallbackData("$prefix;$PAGING_BY_ITEMS;${itemIds.last()}"))
                    }
                }
            }
        } else {
            for (item in 1..itemIds.size) {
                val title = if (selectedItem == item) "·$item·" else "$item"
                row.add(InlineKeyboardButton().setText(title).setCallbackData("$prefix;$PAGING_BY_ITEMS;${itemIds[item - 1]}"))
            }
        }

        return row
    }

    private fun buildBackRow(prefix: String): ArrayList<InlineKeyboardButton> {
        val backRow = ArrayList<InlineKeyboardButton>()

        backRow.add(InlineKeyboardButton().setText("$BACK Back").setCallbackData("$prefix;$PREV_STEP;-1"))

        return backRow
    }

}
