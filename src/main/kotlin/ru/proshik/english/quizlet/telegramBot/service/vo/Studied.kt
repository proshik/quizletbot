package ru.proshik.english.quizlet.telegramBot.service.vo

data class Studied(val studiedClass: StudiedClass,
                   val setsStats: List<StudiedSet>)

data class StudiedClass(val id: Long,
                        val name: String,
                        val url: String)

data class StudiedSet(val id: Long,
                      val title: String,
                      val url: String,
                      val createdDate: Long,
                      val publishedDate: Long,
                      val studiedModes: List<StudiedMode>)


data class StudiedMode(val type: ModeType,
                       val startDate: Long,
                       val finishDate: Long?,
                       val formattedScore: String?)