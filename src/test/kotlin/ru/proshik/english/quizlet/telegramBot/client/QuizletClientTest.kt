package ru.proshik.english.quizlet.telegramBot.client

import com.github.kittinunf.fuel.core.Client
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.io.ByteArrayInputStream
import java.net.URL
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class QuizletClientTest {

    private val client: QuizletClient = QuizletClient("secret", "redirectUrl")

    @Test
    fun getAccessTokenSuccessTest() {
        val resultJson = "{\n" +
                "    \"access_token\": \"W5hMg7vAFdhzke7FdtMAc2jYqMGwyDWJSXVFuxSn\",\n" +
                "    \"expires_in\": 315360000,\n" +
                "    \"token_type\": \"bearer\",\n" +
                "    \"scope\": \"read\",\n" +
                "    \"user_id\": \"Prokhor_Krylov\"\n" +
                "}"
        initFuelManager(resultJson)

        val actualResult = client.accessToken("code")

        assertNotNull(actualResult)
    }

    @Test
    fun userGroupsSuccessTest() {
        val resultJson = "[\n" +
                "  {\n" +
                "    \"id\": 7346553,\n" +
                "    \"url\": \"https://quizlet.com/class/7346553/\",\n" +
                "    \"name\": \"BE A2+ #30 (Veronika Pakeleva)\",\n" +
                "    \"set_count\": 17,\n" +
                "    \"user_count\": 12,\n" +
                "    \"created_date\": 1531510447,\n" +
                "    \"has_access\": true,\n" +
                "    \"access_level\": \"member\",\n" +
                "    \"role_level\": 1,\n" +
                "    \"description\": \"\",\n" +
                "    \"admin_only\": false,\n" +
                "    \"is_public\": false,\n" +
                "    \"has_password\": false,\n" +
                "    \"member_add_sets\": true,\n" +
                "    \"school\": {\n" +
                "      \"name\": \"EPAM Global\",\n" +
                "      \"id\": 2568325,\n" +
                "      \"city\": \"Global\",\n" +
                "      \"state\": null,\n" +
                "      \"country_code\": \"us\",\n" +
                "      \"latitude\": 0,\n" +
                "      \"longitude\": 0\n" +
                "    },\n" +
                "    \"sets\": [\n" +
                "      {\n" +
                "        \"id\": 310192908,\n" +
                "        \"url\": \"https://quizlet.com/310192908/br-int-2ndunit-4vocabulary-flash-cards/\",\n" +
                "        \"title\": \"[BR Int 2nd][Unit 4][Vocabulary]\",\n" +
                "        \"created_by\": \"martsina\",\n" +
                "        \"term_count\": 30,\n" +
                "        \"created_date\": 1535620988,\n" +
                "        \"modified_date\": 1535622747,\n" +
                "        \"published_date\": 1535622747,\n" +
                "        \"has_images\": false,\n" +
                "        \"subjects\": [],\n" +
                "        \"visibility\": \"public\",\n" +
                "        \"editable\": \"only_me\",\n" +
                "        \"has_access\": true,\n" +
                "        \"can_edit\": false,\n" +
                "        \"description\": \"\",\n" +
                "        \"lang_terms\": \"en\",\n" +
                "        \"lang_definitions\": \"en\",\n" +
                "        \"password_use\": 0,\n" +
                "        \"password_edit\": 0,\n" +
                "        \"access_type\": 2,\n" +
                "        \"creator_id\": 70798850,\n" +
                "        \"added_by\": {\n" +
                "          \"username\": \"martsina\",\n" +
                "          \"account_type\": \"teacher\",\n" +
                "          \"profile_image\": \"https://quizlet.com/a/i/animals/26.jqcv.jpg\",\n" +
                "          \"id\": 70798850\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"members\": [\n" +
                "      {\n" +
                "        \"username\": \"Prokhor_Krylov\",\n" +
                "        \"role\": \"member\",\n" +
                "        \"role_level\": 1,\n" +
                "        \"email_notification\": true,\n" +
                "        \"profile_image\": \"https://up.quizlet.com/1fx7cz-qpBDt-256s.jpg\",\n" +
                "        \"membership_id\": 45113387\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "]"
        initFuelManager(resultJson)

        val actualResult = client.userGroups("Prokhor_Krylov", "accessToken")

        assertNotNull(actualResult)
    }

    @Test
    fun userStudiedSuccessTest() {
        val resultJson = "[\n" +
                "  {\n" +
                "    \"mode\": \"speller\",\n" +
                "    \"id\": 7544024208,\n" +
                "    \"start_date\": 1536212803,\n" +
                "    \"finish_date\": null,\n" +
                "    \"formatted_score\": null,\n" +
                "    \"set\": {\n" +
                "      \"id\": 310965083,\n" +
                "      \"url\": \"https://quizlet.com/310965083/br-int-2nd-unit-1-3-skills-flash-cards/\",\n" +
                "      \"title\": \"[BR Int 2nd] [ Unit 1-3] [Skills]\",\n" +
                "      \"created_by\": \"martsina\",\n" +
                "      \"term_count\": 72,\n" +
                "      \"created_date\": 1535982301,\n" +
                "      \"modified_date\": 1535982319,\n" +
                "      \"published_date\": 1535982318,\n" +
                "      \"has_images\": false,\n" +
                "      \"subjects\": [],\n" +
                "      \"visibility\": \"public\",\n" +
                "      \"editable\": \"only_me\",\n" +
                "      \"has_access\": true,\n" +
                "      \"can_edit\": false,\n" +
                "      \"description\": \"\",\n" +
                "      \"lang_terms\": \"en\",\n" +
                "      \"lang_definitions\": \"en\",\n" +
                "      \"password_use\": 0,\n" +
                "      \"password_edit\": 0,\n" +
                "      \"access_type\": 2,\n" +
                "      \"creator_id\": 70798850\n" +
                "    },\n" +
                "    \"image_url\": \"https://quizlet.com/a/i/animals/26.jqcv.jpg\"\n" +
                "  }\n" +
                "]"
        initFuelManager(resultJson)

        val actualResult = client.userStudied("Prokhor_Krylov", "accessToken")

        assertNotNull(actualResult)
    }

    private fun initFuelManager(respJson: String,
                                statusCode: Int = 200,
                                headers: Map<String, List<String>> = emptyMap()) {
        val client = object : Client {
            override fun executeRequest(request: Request): Response {
                return Response(
                        dataStream = ByteArrayInputStream(respJson.toByteArray()),
                        statusCode = statusCode,
                        headers = headers,
                        url = URL("https://www.test.test"),
                        responseMessage = "mess",
                        contentLength = 0L
                )
            }
        }

        FuelManager.instance.client = client
    }

}