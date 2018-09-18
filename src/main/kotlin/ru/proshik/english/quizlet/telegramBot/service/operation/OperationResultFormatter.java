package ru.proshik.english.quizlet.telegramBot.service.operation;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface OperationResultFormatter<T extends OperationResult> {

    BotApiMethod<Message> format(T operationResult);

}
