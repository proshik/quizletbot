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
        val id: Long? = null,

        val createdDate: ZonedDateTime,

        val chatId: String,

        @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL])
        var account: Account?
)
//{
//    fun set(account: Account?) {
//        if (account == null) {
//            if (this.account != null) {
//                this.account!!.set(null)
//            }
//        } else {
//            account.set(this)
//        }
//        this.account = account
//    }
//}