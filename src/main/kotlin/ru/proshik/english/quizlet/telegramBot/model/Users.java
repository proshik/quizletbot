package ru.proshik.english.quizlet.telegramBot.model;

import org.springframework.data.annotation.Id;

public class Users {

    @Id
    private Long id;

    private Long chatId;

    private String login;

    private String accessToken;

    public Users() {
    }

    public Users(Long chatId, String login, String accessToken) {
        this.chatId = chatId;
        this.login = login;
        this.accessToken = accessToken;
    }

    public Long getId() {
        return id;
    }

    public Long getChatId() {
        return chatId;
    }

    public String getLogin() {
        return login;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

}
