package ru.proshik.english.quizlet.telegramBot.model;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;
import java.time.ZonedDateTime;

import static javax.persistence.GenerationType.SEQUENCE;
import static org.hibernate.id.enhanced.SequenceStyleGenerator.SEQUENCE_PARAM;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = SEQUENCE, generator = "users_id_seq")
    @GenericGenerator(name = "users_id_seq",
            strategy = "enhanced-sequence",
            parameters = @Parameter(name = SEQUENCE_PARAM, value = "users_id_seq"))
    private Long id;

    private ZonedDateTime createdDate;

    @Column(name = "chat_id", nullable = false)
    private String chatId;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Account account;

    public User() {
    }

    public User(ZonedDateTime createdDate, String chatId) {
        this.createdDate = createdDate;
        this.chatId = chatId;
    }

    public User(ZonedDateTime createdDate, String chatId, Account account) {
        this.createdDate = createdDate;
        this.chatId = chatId;
        this.account = account;
    }

    public Long getId() {
        return id;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public String getChatId() {
        return chatId;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        if (account == null) {
            if (this.account != null) {
                this.account.setUser(null);
            }
        } else {
            account.setUser(this);
        }
        this.account = account;
    }
}
