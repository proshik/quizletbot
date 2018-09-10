package ru.proshik.english.quizlet.telegramBot.client

import kotlin.test.assertNotNull

/**
 * 1. Write correct properties into {@see QuizletClient} constructor
 * 2. Write correct arguments in each methods
 * 3. Uncomment @Test annotatuin
 * 4. Run test
 */
class QuizletClientRealTest {

    private val client: QuizletClient = QuizletClient(
            "R2Y4UHl4eVJQWTo2asdffmhVY0R3bWcyZHBdfen452sadf",
            "https://quizletsetsinfo.herokuapp.com/redirect")

    //    @Test
    fun getAccessTokenTest() {
        val actualResult = client.accessToken("code")

        assertNotNull(actualResult)
    }

    //    @Test
    fun userGroupsTest() {
        val actualResult = client.userGroups("Prokhor_Krylov", "25hMsdfhzke7FdtMdfjYqdfyDWJSXVFuxSn")

        assertNotNull(actualResult)
    }

    //    @Test
    fun userStudiedTest() {
        val actualResult = client.userStudied("Prokhor_Krylov", "35dfvAdfzke7FdtMdfYqMGdyDWJSdfSn")

        assertNotNull(actualResult)
    }
}
