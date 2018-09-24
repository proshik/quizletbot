package ru.proshik.english.quizlet.telegramBot.demo;

import org.jetbrains.annotations.NotNull;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

import static java.lang.StrictMath.toIntExact;

public class BotApi20 extends TelegramLongPollingBot {

    private Integer prevCount = 0;
    private Integer nextCount = 0;

    @Override
    public void onUpdateReceived(Update update) {

        // We check if the update has a message and the message has text
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chat_id = update.getMessage().getChatId();

            if (update.getMessage().getText().equals("/start")) {
                SendMessage message = new SendMessage() // Create a message object object
                        .setChatId(chat_id)
                        .setText("You send /start");

                InlineKeyboardMarkup markupInline = buildInlineKeyboardMarkup();

                message.setReplyMarkup(markupInline);

                try {
                    execute(message); // Sending our message object to user
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        } else if (update.hasCallbackQuery()) {
            // Set variables
            String call_data = update.getCallbackQuery().getData();
            long message_id = update.getCallbackQuery().getMessage().getMessageId();
            long chat_id = update.getCallbackQuery().getMessage().getChatId();

            if (call_data.equals("previous")) {
                String answer = "prevValue" + ++prevCount;
                EditMessageText new_message = new EditMessageText()
                        .setChatId(chat_id)
                        .setReplyMarkup(buildInlineKeyboardMarkup())
                        .setMessageId(toIntExact(message_id))
                        .setText(answer);
                try {
                    execute(new_message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (call_data.equals("next")) {

                EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();

                String answer = "nextValue" + ++nextCount;
                EditMessageText new_message = new EditMessageText()
                        .setChatId(chat_id)
                        .setReplyMarkup(buildInlineKeyboardMarkup())
                        .setMessageId(toIntExact(message_id))
                        .setText(answer);
                try {
                    execute(new_message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @NotNull
    private InlineKeyboardMarkup buildInlineKeyboardMarkup() {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        rowInline.add(new InlineKeyboardButton().setText("previous").setCallbackData("previous"));
        rowInline.add(new InlineKeyboardButton().setText("next").setCallbackData("next"));

        // Set the keyboard to the markup
        rowsInline.add(rowInline);

        // Add it to the message
        markupInline.setKeyboard(rowsInline);
        return markupInline;
    }

    @Override
    public String getBotUsername() {
        // Return bot username
        // If bot username is @MyAmazingBot, it must return 'MyAmazingBot'
        return "testbot_dev_bot";
    }

    @Override
    public String getBotToken() {
        // Return bot token from BotFather
        return "611962903:AAHq6exkrbRAahM44fLTcyB4wl3aUSoDmbc";
    }
}