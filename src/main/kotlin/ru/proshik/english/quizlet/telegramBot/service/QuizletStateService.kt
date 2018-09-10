package ru.proshik.english.quizlet.telegramBot.service

import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class QuizletStateService {

    private var store: ConcurrentHashMap<String, String> = ConcurrentHashMap()

    fun add(): String {
        val state = generateState()
        store[state] = ""

        return state
    }

    fun add(chatId: String): String {
        val state = generateState()
        store[state] = chatId

        return state
    }

    fun get(state: String): String? {
        return store[state]
    }

    fun delete(state: String): Boolean {
        try {
            store.remove(state)
        } catch (e: NullPointerException) {
            return false
        }

        return true
    }

    private fun generateState(): String {
        return UUID.randomUUID().toString()
    }
}
