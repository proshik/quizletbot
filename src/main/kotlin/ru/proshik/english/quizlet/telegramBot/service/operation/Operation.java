package ru.proshik.english.quizlet.telegramBot.service.operation;

public interface Operation<T extends OperationResult, R extends OperationResultFormatter> {

    T init();

    OperationResult<?, ?> nextStep(String text);

    R getFormatter();
}
