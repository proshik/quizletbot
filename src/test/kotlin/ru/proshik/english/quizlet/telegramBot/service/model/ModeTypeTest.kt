package ru.proshik.english.quizlet.telegramBot.service.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ModeTypeTest {

    @Test
    fun modeTypeByDesignationSuccess() {
        val actualResult = ModeType.modeTypeByDesignation()

        assertEquals(10, actualResult.size)
        assertEquals(ModeType.WRITE, actualResult["mobile_learn"])
    }

    @Test
    fun modeTypeByDesignationByFlashcardValueSuccess() {
        val actualResult = ModeType.modeTypeByDesignation("flashcards")

        assertEquals(ModeType.FLASHCARDS, actualResult)
    }

}