package ru.proshik.english.quizlet.telegramBot.service.operation

import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import java.io.Serializable

interface Operation {

    fun initOperation(chatId: Long): OperationStepInfo?

    fun nextSubOperation(chatId: Long,
                         messageId: Int,
                         callData: String,
                         operationPipeline: OperationPipeline): BotApiMethod<out Serializable>

}

data class OperationStepInfo(val pipeline: OperationPipeline,
                             val outputData: List<Pair<String, String>>)
