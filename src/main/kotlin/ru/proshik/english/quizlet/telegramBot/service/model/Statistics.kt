package ru.proshik.english.quizlet.telegramBot.service.model

data class Statistics(val groupId: Long,
                      val groupName: String,
                      val setsStats: List<SetStat>)

data class SetStat(val id: Long,
                   val title: String,
                   val url: String,
                   val createdDate: Long,
                   val publishedDate: Long,
                   val modeStats: List<ModeStat>)


data class ModeStat(val mode: ModeType,
                    val startDate: Long,
                    val finishDate: Long?,
                    val formattedScore: String?)