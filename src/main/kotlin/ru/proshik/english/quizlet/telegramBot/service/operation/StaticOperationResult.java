package ru.proshik.english.quizlet.telegramBot.service.operation;

import ru.proshik.english.quizlet.telegramBot.service.model.Statistics;

import java.util.List;

public class StaticOperationResult implements OperationResult<List<String>, Statistics> {

    private List<String> step;

    private Statistics done;

    private boolean isTerminate;

    public StaticOperationResult(boolean isTerminate) {
        this.isTerminate = isTerminate;
    }

    public StaticOperationResult(List<String> step, boolean isTerminate) {
        this.step = step;
        this.isTerminate = isTerminate;
    }

    public StaticOperationResult(Statistics done, boolean isTerminate) {
        this.done = done;
        this.isTerminate = isTerminate;
    }

    @Override
    public boolean isTerminate() {
        return isTerminate;
    }

    @Override
    public List<String> step() {
        return step;
    }

    @Override
    public Statistics done() {
        return done;
    }

}
