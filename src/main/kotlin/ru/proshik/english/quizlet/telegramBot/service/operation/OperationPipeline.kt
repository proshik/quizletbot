package ru.proshik.english.quizlet.telegramBot.service.operation

data class OperationPipeline(val type: OperationType,
                             val step: OperationStep,
                             val data: Map<String, String>? = null)