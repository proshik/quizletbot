package ru.proshik.english.quizlet.telegramBot.service.operation;

import ru.proshik.english.quizlet.telegramBot.service.QuizletInfoService;

public class StaticOperationProvider {

    private QuizletInfoService quizletInfoService;

    public StaticOperationProvider(QuizletInfoService quizletInfoService) {
        this.quizletInfoService = quizletInfoService;
    }

    public QuizletInfoService getQuizletInfoService() {
        return quizletInfoService;
    }
}
