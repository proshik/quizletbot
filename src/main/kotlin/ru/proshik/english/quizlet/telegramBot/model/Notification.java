package ru.proshik.english.quizlet.telegramBot.model;


import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;

import static javax.persistence.GenerationType.SEQUENCE;
import static org.hibernate.id.enhanced.SequenceStyleGenerator.SEQUENCE_PARAM;

@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = SEQUENCE, generator = "notification_id_seq")
    @GenericGenerator(name = "notification_id_seq",
            strategy = "enhanced-sequence",
            parameters = @Parameter(name = SEQUENCE_PARAM, value = "notification_id_seq"))
    private Long id;

    private ZonedDateTime createdDate;

    @Enumerated(value = EnumType.STRING)
    private NotificationType type;

    @Enumerated(value = EnumType.STRING)
    private DayOfWeek dayOfWeek;

    // from 0 to 23
    private int hour;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    public Notification() {
    }

    public Notification(ZonedDateTime createdDate, NotificationType type, DayOfWeek dayOfWeek, int hour) {
        this.createdDate = createdDate;
        this.type = type;
        this.dayOfWeek = dayOfWeek;
        this.hour = hour;
    }

    public Long getId() {
        return id;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public NotificationType getType() {
        return type;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public int getHour() {
        return hour;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
