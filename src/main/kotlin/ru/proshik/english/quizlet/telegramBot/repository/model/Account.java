package ru.proshik.english.quizlet.telegramBot.repository.model;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;
import java.time.ZonedDateTime;

import static javax.persistence.GenerationType.SEQUENCE;
import static org.hibernate.id.enhanced.SequenceStyleGenerator.SEQUENCE_PARAM;

@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = SEQUENCE, generator = "account_id_seq")
    @GenericGenerator(name = "account_id_seq",
            strategy = "enhanced-sequence",
            parameters = @Parameter(name = SEQUENCE_PARAM, value = "account_id_seq"))
    private Long id;

    private ZonedDateTime createdDate;

    private String login;

    private String accessToken;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Account() {
    }

    public Account(ZonedDateTime createdDate, String login, String accessToken) {
        this.createdDate = createdDate;
        this.login = login;
        this.accessToken = accessToken;
    }

    public Long getId() {
        return id;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public String getLogin() {
        return login;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
