package ru.proshik.english.quizlet.telegramBot.service.operation

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import ru.proshik.english.quizlet.telegramBot.service.QuizletInfoService
import ru.proshik.english.quizlet.telegramBot.service.action.MessageFormatter
import java.io.Serializable

@Component
class StudiedOperation(val quizletInfoService: QuizletInfoService) : Operation {

    val messageFormatter = MessageFormatter()

    enum class StudiedOperationStep : OperationStep {
        SELECT_GROUP,
        SELECT_SET;
    }

    override fun initOperation(chatId: Long): OperationStepInfo? {
        val userGroups = quizletInfoService.userGroups(chatId)

        return when {
            userGroups.size > 1 -> {
                val outputData = userGroups.asSequence()
                        .map { group -> Pair(group.name, group.id.toString()) }
                        .toList()

                val operationInfo = OperationPipeline(OperationType.NOTIFICATIONS, StudiedOperationStep.SELECT_GROUP)

                OperationStepInfo(operationInfo, outputData)
            }
            userGroups.size == 1 -> {
                val outputData = userGroups[0].sets.asSequence()
                        .sortedByDescending { set -> set.publishedDate }
                        .map { set -> Pair(set.title, set.id.toString()) }
                        .toList()


                val data = mapOf("group_id" to userGroups[0].id.toString())
                val operationInfo = OperationPipeline(OperationType.NOTIFICATIONS, StudiedOperationStep.SELECT_SET, data)

                OperationStepInfo(operationInfo, outputData)
            }
            userGroups.isEmpty() -> null
            else -> throw RuntimeException("unreacheble path for initialize studied opeartion")
        }
    }

    override fun nextSubOperation(chatId: Long, messageId: Int, callData: String, operationPipeline: OperationPipeline): BotApiMethod<out Serializable> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun navigateBySteps(chatId: Long,
                        messageId: Int,
                        messageText: String,
                        items: List<Pair<String, String>>,
                        itemInRow: Int = 1,
                        itemFrom: Int = 1,
                        itemTill: Int = 5,
                        showAllLine: Boolean = false,
                        isNewMessage: Boolean = true): BotApiMethod<out Serializable> {

        // check input params
        if (items.isEmpty()) throw IllegalArgumentException("unexpectable count of values items=${items.size}")
        if (itemFrom >= itemTill) throw IllegalArgumentException("itemFrom=$itemFrom equals or more itemTill=$itemTill")
        if (itemInRow < 1) throw IllegalArgumentException("unexpectable value itemInRow=$itemInRow")
        if (itemInRow > 1 && itemInRow < (itemTill - itemFrom))
            throw IllegalArgumentException("incompatible values itemInRow=$itemInRow and borders itemFrom=$itemFrom, itemTill=$itemTill")

        // calculate value of items on one line
        val countItemOnLine = if (itemInRow != 1 && items.size > itemInRow) itemInRow else items.size
        // initialize rows
        val rows = ArrayList<List<InlineKeyboardButton>>()
        // check and add the first line with a title "All items"
        if (showAllLine && items.size > 1) {
            val allElementRow = ArrayList<InlineKeyboardButton>()
            allElementRow.add(InlineKeyboardButton().setText("All items").setCallbackData("-1"))
            rows.add(allElementRow)
        }

        // main block. Fill items
        var i = 1
        var itemRows = ArrayList<InlineKeyboardButton>()
        for (item in items) {
            itemRows.add(InlineKeyboardButton().setText(item.first).setCallbackData(item.second))

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
//        var navigateRow = ArrayList<InlineKeyboardButton>()
//        for (i in itemFrom..itemTill) {
//            navigateRow.add(InlineKeyboardButton().setText(item.first).setCallbackData(item.second))
//        }
//        rows.add(navigateRow)


        val keyboard = InlineKeyboardMarkup()
        keyboard.keyboard = rows

        return if (isNewMessage)
            SendMessage().setChatId(chatId).setText(messageText).setReplyMarkup(keyboard)
        else
            EditMessageText().setChatId(chatId).setMessageId(messageId).setText(messageText).setReplyMarkup(keyboard)
//            EditMessageReplyMarkup().setChatId(chatId).setReplyMarkup(keyboard).setMessageId(messageId)
    }

    fun navigateByItems(chatId: Long,
                        messageId: Int,
                        messageText: String,
                        items: List<Pair<String, String>>,
                        currentItem: Int = 0): BotApiMethod<out Serializable> {

        // check input params
        if (items.isEmpty()) throw IllegalArgumentException("unexpectable count of values items=${items.size}")

        // initialize rows
        val rows = ArrayList<List<InlineKeyboardButton>>()

        // main block. Fill items
        var i = 1
        var numberRow = ArrayList<InlineKeyboardButton>()
        for (item in 0..2) {
//            numberRow.add(InlineKeyboardButton().setText(item.first).setCallbackData(item.second))
//
//            if (i % countItemOnLine == 0) {
//                rows.add(numberRow)
//                i = 1
//                numberRow = ArrayList()
//            } else {
//                i++
//            }
        }
        rows.add(numberRow)

        // add buttons to navigate
//        var navigateRow = ArrayList<InlineKeyboardButton>()
//        for (i in itemFrom..itemTill) {
//            navigateRow.add(InlineKeyboardButton().setText(item.first).setCallbackData(item.second))
//        }
//        rows.add(navigateRow)


        val keyboard = InlineKeyboardMarkup()
        keyboard.keyboard = rows

        return EditMessageText().setChatId(chatId).setMessageId(messageId).setText(messageText).setReplyMarkup(keyboard)
    }

    private fun formatStep(chatId: Long, items: List<String>,
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