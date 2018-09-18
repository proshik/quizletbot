package ru.proshik.english.quizlet.telegramBot.service.command;

public class StatisticCommand implements Command {

    public enum Step {
        SELECT_GROUP,
        SELECT_SET
    }

    private Long chatId;

    private Step currentStep;

    private Long groupId;

    public StatisticCommand() {
    }

    public StatisticCommand(Long chatId) {
        this.chatId = chatId;
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

    public Step getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(Step currentStep) {
        this.currentStep = currentStep;
    }
}
