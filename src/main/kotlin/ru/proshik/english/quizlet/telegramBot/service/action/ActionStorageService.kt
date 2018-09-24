package ru.proshik.english.quizlet.telegramBot.service.action

import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class ActionStorageService {

    val actionInfoQueue = ConcurrentHashMap<Long, ActionInfo>()

    fun saveActionInfo(chatId: Long, actionInfo: ActionInfo) {
        actionInfoQueue[chatId] = actionInfo
    }

    fun getActionInfo(chatId: Long): ActionInfo? {
        return actionInfoQueue[chatId]
    }

    fun removeActionInfo(chatId: Long){
        actionInfoQueue.remove(chatId)
    }

}