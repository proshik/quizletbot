package ru.proshik.english.quizlet.telegramBot.model;

import org.springframework.data.annotation.Id;

import java.time.DayOfWeek;


public class Notification {

    @Id
    private Long id;

    private NotificationType type;

    private DayOfWeek dayOfWeek;
    // from 0 to 23
    private int hour;

    public Notification() {
    }

    public Notification(NotificationType type, DayOfWeek dayOfWeek, int hour) {
        this.type = type;
        this.dayOfWeek = dayOfWeek;
        this.hour = hour;
    }

    public Long getId() {
        return id;
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

}
