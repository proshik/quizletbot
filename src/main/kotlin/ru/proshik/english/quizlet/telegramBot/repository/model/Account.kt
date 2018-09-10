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
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_id_seq")
        @GenericGenerator(name = "users_id_seq",
                strategy = "enhanced-sequence",
                parameters = [Parameter(name = SequenceStyleGenerator.SEQUENCE_PARAM, value = "users_id_seq")])
        val id: Long? = null,

        val createdDate: ZonedDateTime,

        val login: String,

        @Column(name = "access_token")
        val accessToken: String,

        @OneToOne @MapsId
        var user: User?
)
