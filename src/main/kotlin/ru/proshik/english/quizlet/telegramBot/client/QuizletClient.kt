package ru.proshik.english.quizlet.telegramBot.client

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.jackson.jacksonDeserializerOf
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ru.proshik.english.quizlet.telegramBot.dto.AuthorizationResp
import ru.proshik.english.quizlet.telegramBot.dto.UserGroupsResp
import ru.proshik.english.quizlet.telegramBot.dto.UserStudiedResp

@Component
class QuizletClient(@Value("\${quizlet.auth.secret}") private val secret: String,
                    @Value("\${quizlet.redirect-url}") private val redirectUrl: String) {
    init {
        FuelManager.instance.apply {
            timeoutInMillisecond = 3000
            timeoutReadInMillisecond = 1000

        }
    }

    companion object {
        const val API_QUIZLET_OAUTH_HEADER_TYPE = "Basic"
        const val API_QUIZLET_AUTH_HEADER_TYPE = "Bearer"

        const val API_QUIZLET_OAUTH_TOKEN = "https://api.quizlet.com/oauth/token"
        const val API_QUIZLET_USERS_GROUP = "https://api.quizlet.com/2.0/users/%s/groups"
        const val API_QUIZLET_USERS_STUDIED = "https://api.quizlet.com/2.0/users/%s/studied"

        val API_QUERY_PARAM_WHITESPACE = Pair("whitespace", "true")
        val API_QUERY_PARAM_DISTINCT = Pair("distinct", "true")
    }

    fun accessToken(code: String): AuthorizationResp {
        val parameters = listOf(
                Pair("grant_type", "authorization_code"),
                Pair("redirect_uri", redirectUrl),
                Pair("code", code))

        return Fuel.get(API_QUIZLET_OAUTH_TOKEN, parameters)
                .header(Pair("Authorization", "$API_QUIZLET_OAUTH_HEADER_TYPE $secret"))
                .responseObject(jacksonDeserializerOf<AuthorizationResp>())
                .third
                .fold({ value -> value }, { fuelError -> throw RuntimeException(fuelError) })
    }

    fun userGroups(userName: String, accessToken: String): List<UserGroupsResp> {
        return Fuel.get(API_QUIZLET_USERS_GROUP.format(userName), listOf(API_QUERY_PARAM_WHITESPACE, API_QUERY_PARAM_WHITESPACE))
                .header(Pair("Authorization", "$API_QUIZLET_AUTH_HEADER_TYPE $accessToken"))
                .responseObject(jacksonDeserializerOf<List<UserGroupsResp>>())
                .third
                .fold({ list -> list }, { fuelError -> throw RuntimeException(fuelError) })
    }

    fun userStudied(userName: String, accessToken: String): List<UserStudiedResp> {
        return Fuel.get(API_QUIZLET_USERS_STUDIED.format(userName), listOf(API_QUERY_PARAM_DISTINCT))
                .header(Pair("Authorization", "$API_QUIZLET_AUTH_HEADER_TYPE $accessToken"))
                .responseObject(jacksonDeserializerOf<List<UserStudiedResp>>())
                .third
                .fold({ list -> list }, { fuelError -> throw RuntimeException(fuelError) })
    }

}
