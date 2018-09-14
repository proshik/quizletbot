package ru.proshik.english.quizlet.telegramBot.service.model

data class StudiedModes(val mode: String,
                        val startDate: Long,
                        val finishDate: Long?,
                        val formattedScore: String?)