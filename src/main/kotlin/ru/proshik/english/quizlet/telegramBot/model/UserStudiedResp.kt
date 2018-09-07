package ru.proshik.english.quizlet.telegramBot.model

import com.fasterxml.jackson.annotation.JsonProperty

class UserStudiedResp(val mode: String,
                      val id: Long,
                      @JsonProperty("start_date") val startDate: Long,
                      @JsonProperty("finish_date") val finishDate: Long,
                      @JsonProperty("formatted_score") val formattedScore: String?,
                      val set: SetResp)
