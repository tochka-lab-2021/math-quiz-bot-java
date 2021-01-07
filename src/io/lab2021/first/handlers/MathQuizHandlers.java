package io.lab2021.first.handlers;

import io.lab2021.first.model.State;
import io.lab2021.first.model.Task;
import io.lab2021.first.services.StateService;
import io.lab2021.first.services.TaskService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MathQuizHandlers extends TelegramLongPollingBot {
    private String token = "";

    private static final String START = "/start";
    private static final String HELP = "/help";
    private static final String ANY = "any";
    private static final String CALLBACK = "callback";

    private TaskService taskSrv;
    private StateService stateSrv;

    public MathQuizHandlers() {
        super();
        taskSrv = new TaskService();
        stateSrv = new StateService();
    }

    public void setConnection(Connection conn) {
        stateSrv.setConnection(conn);
    }

    @Override
    public String getBotUsername() {
        return "math-azaza";
    }

    @Override
    public String getBotToken() {
        // See https://github.com/tochka-lab-2021/math-quiz-bot/blob/main/bot.py#L129-L132
        if (this.token.isEmpty()) {
            // Read telegram token from token.txt file.
            List<String> lines = new ArrayList<>();
            Path path = Paths.get("token.txt");

            try (Stream<String> lineStream = Files.lines(path)) {
                lines = lineStream.collect(Collectors.toList());
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.token = lines.get(0);
        }
        return this.token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();
                if (message.hasText() && message.getText().equals(HELP) ) {
                    // See https://github.com/tochka-lab-2021/math-quiz-bot/blob/main/bot.py#L135-L136
                    // Show help.
                    onHelp(message);
                    return;
                }
                if (message.hasText() && message.getText().equals(START) ) {
                    // See https://github.com/tochka-lab-2021/math-quiz-bot/blob/main/bot.py#L140-L141
                    // Generate new task or show current.
                    onStart(message);
                    return;
                }
                // See https://github.com/tochka-lab-2021/math-quiz-bot/blob/main/bot.py#L155-L156
                // Check answer.
                onAny(message);
                return;
            }
            if (update.hasCallbackQuery()) {
                // See https://github.com/tochka-lab-2021/math-quiz-bot/blob/main/bot.py#L190-L191
                // Generate new task and hide keyboard.
                CallbackQuery query = update.getCallbackQuery();
                onCallbackQuery(query);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onHelp(Message message) {
        // See https://github.com/tochka-lab-2021/math-quiz-bot/blob/main/bot.py#L136-L137
        // Send reply with instructions.
        replyTo(message, "Чтобы начать викторину, нажми кнопку 'Новая задача' или отправь любое сообщение.");
    }

    private void onStart(Message message) {
        // See https://github.com/tochka-lab-2021/math-quiz-bot/blob/main/bot.py#L141-L152
        // Get user state, check if there is a current task, gen new task.
        Integer userId = message.getFrom().getId();
        State state = stateSrv.getUserState(userId);

        if (state.getTask() == null) {
            // Send welcome message with inline keyboard.
            Message startMsg = sendMessage(message, "Привет! Давай порешаем задачки?", newTaskMarkup());
            // Remove keyboard from earlier message.
            removeReplyMarkup(message.getChatId(), state, startMsg);
            stateSrv.saveUserState(state);
        } else {
            // See https://github.com/tochka-lab-2021/math-quiz-bot/blob/main/bot.py#L152
            replyTo(message, String.format("Задана задача\n%s", state.getTask().getTask()));
        }
    }

    private void onAny(Message message) {
        // See https://github.com/tochka-lab-2021/math-quiz-bot/blob/main/bot.py#L192-L212
        Integer userId = message.getFrom().getId();
        State state = stateSrv.getUserState(userId);

        if (state.getTask() == null) {
            // Generate new task and show to user.
            Task task = taskSrv.genTask();
            state.setTask(task);
            state.setTries(0);
            state.setUserId(userId);
            sendMessage(message, task.getTask());
            // Remove keyboard from earlier message.
            removeReplyMarkup(message.getChatId(), state, null);
            stateSrv.saveUserState(state);
        } else {
            // Check answer
            if (message.getText().equals(state.getTask().getAnswer())) {
                // Correct answer: congrats to user, show button
                Message msg = sendMessage(message,
                        String.format("И правда, %s=%s. Продолжим?", state.getTask().getTask(), message.getText()),
                        newTaskMarkup());
                // Remove keyboard from earlier message.
                removeReplyMarkup(message.getChatId(), state, msg);
                state.setTask(null);
                state.setTries(0);
                state.setUserId(userId);
                stateSrv.saveUserState(state);
            } else {
                // Wrong answer: message to user, show keyboard.
                Message wrongMsg = sendMessage(message,
                        "Неверный ответ, попробуйте ещё раз.",
                        newTaskMarkup());
                // Remove keyboard from earlier message.
                removeReplyMarkup(message.getChatId(), state, wrongMsg);
                // state.setTries(state.getTries()+1);
                stateSrv.saveUserState(state);
            }
        }
    }

    private void onCallbackQuery(CallbackQuery call) {
        if (!call.getData().equals("give_up")) {
            return;
        }
        answerCallbackQuery(call.getId());

        // Get user state, check if there is a current task, gen new task.
        Integer userId = call.getFrom().getId();
        State state = stateSrv.getUserState(userId);

        // Generate new task and show to user.
        Task task = taskSrv.genTask();
        state.setTask(task);
        state.setTries(0);
        state.setUserId(userId);
        sendMessage(call.getMessage(), task.getTask());

        // Remove button from saved message id.
        if (state.getMessageWithInlineKeyboardId() != null) {
            if (state.getMessageWithInlineKeyboardId().longValue() != call.getMessage().getMessageId().longValue()) {
                removeReplyMarkup(call.getMessage().getChatId(), state, null);
            }
        }

        // Remove clicked button.
        editMessageReplyMarkup(call.getMessage().getChatId(), call.getMessage().getMessageId(), null);
        state.setMessageWithInlineKeyboardId(null);
        stateSrv.saveUserState(state);
    }


    private InlineKeyboardMarkup newTaskMarkup() {
        // See https://github.com/tochka-lab-2021/math-quiz-bot/blob/main/bot.py#L113-L117
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        // Create button
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText("Новая задача");
        btn.setCallbackData("give_up");
        // Set the button and the keyboard to the markup
        rowInline.add(btn);
        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);
        return markupInline;
    }


    private void removeReplyMarkup(Long chatId, State state, Message newMsg) {
        // See https://github.com/tochka-lab-2021/math-quiz-bot/blob/main/bot.py#L120-L126
        if (state.getMessageWithInlineKeyboardId() != null) {
            // Remove keyboard from previous message.
            editMessageReplyMarkup(chatId, state.getMessageWithInlineKeyboardId(), null);
            state.setMessageWithInlineKeyboardId(null);
        }
        if (newMsg != null) {
            // Save last message id.
            state.setMessageWithInlineKeyboardId(newMsg.getMessageId());
        }

    }

    private void editMessageReplyMarkup(Long chatId, Integer messageId, InlineKeyboardMarkup replyMarkup) {
        // See https://github.com/tochka-lab-2021/math-quiz-bot/blob/main/bot.py#L120-L126
        EditMessageReplyMarkup reply = new EditMessageReplyMarkup();
        reply.setChatId(chatId.toString());
        reply.setMessageId(messageId);
        reply.setReplyMarkup(replyMarkup);
        try {
            execute(reply);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void sendMessage(Message message, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(message.getChatId()));
        sendMessage.setText(text);
        try {
            execute(sendMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Message sendMessage(Message message, String text, InlineKeyboardMarkup inlineMarkup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(message.getChatId()));
        sendMessage.setText(text);
        sendMessage.setReplyMarkup(inlineMarkup);
        try {
            return execute(sendMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void replyTo(Message message, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(message.getChatId()));
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(text);
        try {
            execute(sendMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void answerCallbackQuery(String callbackQueryId) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQueryId);
        try {
            execute(answer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
