package ru.proshik.english.quizlet.telegramBot.model

import com.fasterxml.jackson.annotation.JsonProperty

data class SetResp(val id: Int,
                   val url: String,
                   val title: String,
                   @JsonProperty("created_date") val createdDate: Long,
                   @JsonProperty("modified_date") val modifiedDate: Long,
                   @JsonProperty("published_date") val publishedDate: Long,
                   @JsonProperty("creator_id") val creatorId: Long,
                   @JsonProperty("created_by") val createdBy: String)