package io.lab2021.first;

import io.lab2021.first.handlers.MathQuizHandlers;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class Main {
    public static void main(String[] args) {
        System.out.println("Connect to db.");
        Connection connection = null;
        try {
            // See https://github.com/tochka-lab-2021/math-quiz-bot/blob/main/bot.py#L219
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(
                    "jdbc:postgresql://localhost/postgres", "diafour", "");

            // See https://github.com/tochka-lab-2021/math-quiz-bot/blob/main/bot.py#L221-L225
            // Test connection.
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT count(1) FROM state;");
            if (rs.next()) {
                String count = rs.getString(1);
                System.out.format("%s users in database.\n", count);
            }
            st.close();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (connection == null) {
            return;
        }

        MathQuizHandlers mathQuiz = new MathQuizHandlers();
        mathQuiz.setConnection(connection);

        System.out.println("Start bot.");
        try {
            // See https://github.com/tochka-lab-2021/math-quiz-bot/blob/main/bot.py#L227-L228
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(mathQuiz);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
