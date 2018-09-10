package ru.proshik.english.quizlet.telegramBot.repository.model

import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Parameter
import org.hibernate.id.enhanced.SequenceStyleGenerator
import java.time.ZonedDateTime
import javax.persistence.*


@Entity
@Table(name = "account")
data class Account(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_id_seq")
        @GenericGenerator(name = "account_id_seq",
                strategy = "enhanced-sequence",
                parameters = [Parameter(name = SequenceStyleGenerator.SEQUENCE_PARAM, value = "account_id_seq")])
        val id: Long?,
        val createdDate: ZonedDateTime,
        val chatId: String,
        @OneToOne @JoinColumn(name = "user_id") val user: User
)
