package ru.proshik.english.quizlet.telegramBot.queue

import org.apache.log4j.Logger
import org.springframework.stereotype.Component
import java.util.concurrent.LinkedBlockingDeque

@Component
class NotificationQueue : Queue {

    companion object {

        private val LOG = Logger.getLogger(NotificationQueue::class.java)
    }

    private val blockingQueue = LinkedBlockingDeque<Queue.Message>()

    override fun put(message: Queue.Message) {
        blockingQueue.add(message)
    }

    override fun take(): Queue.Message {
        return blockingQueue.take()
    }

}
