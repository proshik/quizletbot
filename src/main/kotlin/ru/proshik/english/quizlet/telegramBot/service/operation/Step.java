package ru.proshik.english.quizlet.telegramBot.service.operation;

public interface Step<IN extends StepIn, OUT extends StepOut> {

    OUT next(IN in);

}
