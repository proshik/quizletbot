package ru.proshik.english.quizlet.telegramBot.service.command;

import ru.proshik.english.quizlet.telegramBot.dto.UserGroupsResp;

import java.util.List;

public class StatisticCommand implements Command<List<UserGroupsResp>> {

    private Long chatId;

    private Long groupId;

    private List<Long> setIds;

    private List<UserGroupsResp> meta;

    public StatisticCommand() {
    }

    public StatisticCommand(Long chatId) {
        this.chatId = chatId;
    }

    public StatisticCommand(Long chatId, List<UserGroupsResp> meta) {
        this.chatId = chatId;
        this.meta = meta;
    }

    public StatisticCommand(Long chatId, Long groupId, List<Long> setIds) {
        this.chatId = chatId;
        this.groupId = groupId;
        this.setIds = setIds;
    }

    @Override
    public List<UserGroupsResp> getMeta() {
        return meta;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public List<Long> getSetIds() {
        return setIds;
    }

    public void setSetIds(List<Long> setIds) {
        this.setIds = setIds;
    }
}
