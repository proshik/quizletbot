package ru.proshik.english.quizlet.telegramBot.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class AuthorizationResp(@JsonProperty("access_token") val accessToken: String,
                             @JsonProperty("expires_in") val expiresIn: Long,
                             @JsonProperty("token_type") val tokenType: String,
                             val scope: String,
                             @JsonProperty("user_id") val userId: String)
