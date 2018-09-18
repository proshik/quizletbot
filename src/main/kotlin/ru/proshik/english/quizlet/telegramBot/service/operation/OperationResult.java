package ru.proshik.english.quizlet.telegramBot.service.operation;

public interface OperationResult<STEP, DONE> {

    boolean isTerminate();

    STEP step();

    DONE done();
}
