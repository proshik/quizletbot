package ru.proshik.english.quizlet.telegramBot.repository.model

import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class User(
        @Id val id: Long?
)
