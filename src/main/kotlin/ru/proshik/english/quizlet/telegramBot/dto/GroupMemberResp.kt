package ru.proshik.english.quizlet.telegramBot.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class GroupMemberResp(val username: String,
                           val role: String,
                           @JsonProperty("role_level") val roleLevel: String,
                           @JsonProperty("membership_id") val membershipId: Long)