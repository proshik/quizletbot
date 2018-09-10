package ru.proshik.english.quizlet.telegramBot.repository.model

import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Parameter
import org.hibernate.id.enhanced.SequenceStyleGenerator.SEQUENCE_PARAM
import java.time.ZonedDateTime
import javax.persistence.*
import javax.persistence.GenerationType.SEQUENCE

@Entity
@Table(name = "users")
data class User(
        @Id
        @GeneratedValue(strategy = SEQUENCE, generator = "users_id_seq")
        @GenericGenerator(name = "users_id_seq",
                strategy = "enhanced-sequence",
                parameters = [Parameter(name = SEQUENCE_PARAM, value = "users_id_seq")])
        val id: Long?,
        val createdDate: ZonedDateTime,
        val chatId: String,
        @MapsId
        @OneToOne(mappedBy = "user", optional = false, cascade = [CascadeType.ALL]) val account: Account?
)


