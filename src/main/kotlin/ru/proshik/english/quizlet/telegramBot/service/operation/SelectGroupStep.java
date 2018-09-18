package ru.proshik.english.quizlet.telegramBot.service.operation;

import ru.proshik.english.quizlet.telegramBot.dto.UserGroupsResp;

import java.util.List;

public class SelectGroupStep implements Step<List<UserGroupsResp>, List<String>> {

    @Override
    public List<String> next(List<UserGroupsResp> userGroups) {
        return null;
    }
}
