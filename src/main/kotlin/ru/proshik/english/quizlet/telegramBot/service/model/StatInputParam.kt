package ru.proshik.english.quizlet.telegramBot.service.model

data class StatInputParam(val groupIds: List<Long> = emptyList(),
                          val setIds: List<Long> = emptyList(),
                          val modes: List<ModeType> = emptyList())