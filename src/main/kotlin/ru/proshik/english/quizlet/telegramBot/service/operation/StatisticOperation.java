package ru.proshik.english.quizlet.telegramBot.service.operation;

import ru.proshik.english.quizlet.telegramBot.dto.SetResp;
import ru.proshik.english.quizlet.telegramBot.dto.UserGroupsResp;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class StatisticOperation implements Operation<StaticOperationResult, OperationResultFormatter> {

    enum STEP {
        SELECT_GROUP,
        SELECT_SET
    }

    /**
     * Chat id of the user
     */
    private Long chatId;

    /**
     * Number of current step, where 0 - initial state
     */
    private STEP currentStep;

    private StaticOperationProvider staticOperationProvider;

    private OperationResultFormatter operationResultFormatter;

    private LinkedList<Step> steps = new LinkedList<>();

    private List<UserGroupsResp> userGroups;

    public StatisticOperation(Long chatId, StaticOperationProvider staticOperationProvider) {
        this.chatId = chatId;
        this.staticOperationProvider = staticOperationProvider;

        operationResultFormatter = new StaticOperationResultFormatter(chatId.toString());
    }

    @Override
    public StaticOperationResult init() {
        userGroups = staticOperationProvider.getQuizletInfoService().userGroups(chatId);

        if (userGroups.isEmpty()) {
            return new StaticOperationResult(true);
        } else if (userGroups.size() == 1) {
            List<String> setTitles = new ArrayList<>();
            for (SetResp set : userGroups.stream().flatMap(g -> g.getSets().stream()).collect(Collectors.toList())) {
                setTitles.add(set.getTitle());
            }

            currentStep = STEP.SELECT_SET;

            return new StaticOperationResult(setTitles, false);
        } else {
            List<String> groupNames = new ArrayList<>();
            for (UserGroupsResp group : userGroups) {
                groupNames.add(group.getName());
            }

            currentStep = STEP.SELECT_GROUP;

            return new StaticOperationResult(groupNames, false);
        }
    }

    @Override
    public OperationResult<?, ?> nextStep(String text, String data) {
        switch (currentStep) {
            case SELECT_GROUP:
                // build info for sets
                break;
            case SELECT_SET:
                // build final statistic

                userGroups.stream().filter(group -> group.get)

                staticOperationProvider.getQuizletInfoService().buildStatistic(chatId, text,)
                break;
            default:
                throw new IllegalArgumentException("unexpected current step = " + currentStep);
        }
    }

    @Override
    public OperationResultFormatter getFormatter() {
        return operationResultFormatter;
    }
}
