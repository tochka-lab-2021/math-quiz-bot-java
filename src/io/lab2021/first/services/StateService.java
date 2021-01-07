package io.lab2021.first.services;

import io.lab2021.first.model.State;
import io.lab2021.first.model.Task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class StateService {
    // https://github.com/tochka-lab-2021/math-quiz-bot/blob/main/bot.py#L24
    public Connection conn;

    // https://github.com/tochka-lab-2021/math-quiz-bot/blob/main/bot.py#L26
    public Map<Integer, State> stateStorage;

    public StateService() {
        stateStorage = new HashMap<>();
    }

    public void setConnection(Connection conn) {
        this.conn = conn;
    }

    /**
     * Get state from database or return fresh state if no records for the user.
     *
     * @param userId A unique identifier for user
     * @return State
     */
    public State getUserState(Integer userId) {
        // See https://github.com/tochka-lab-2021/math-quiz-bot/blob/main/bot.py#L29-L67
        try {
            PreparedStatement st = conn.prepareStatement("""
            SELECT
                user_id, task, answer, tries
            FROM
                state
            WHERE
                user_id=?
            """);
            st.setInt(1, userId);
            ResultSet rs = st.executeQuery();
            if (!rs.next()) {
                rs.close();
                st.close();
                State s = new State();
                s.setNewState(true);
                return s;
            }

            Task t = null;
            if (rs.getString(2) != null) {
                t = new Task();
                t.setTask(rs.getString(2));
                t.setAnswer(rs.getString(3));
            }

            State s = new State();
            s.setUserId(userId);
            s.setTask(t);
            s.setTries(rs.getInt(4));

            if (stateStorage.containsKey(userId)) {
                s.setMessageWithInlineKeyboardId(stateStorage.get(userId).getMessageWithInlineKeyboardId());
            }
            return s;
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Should not happen.
        return new State();
    }

    /**
     * Insert a new state in the database or update an existing record.
     *
     * @param state
     */
    public void saveUserState(State state) {
        // See https://github.com/tochka-lab-2021/math-quiz-bot/blob/main/bot.py#L70-L110
        Task task = new Task();
        task.setTask(null);
        task.setAnswer(null);
        if (state.getTask() != null) {
            task = state.getTask();
        }

        try {
            if (state.getNewState()) {
                // Insert
                PreparedStatement st = conn.prepareStatement("""
                INSERT INTO
                    state
                VALUES
                    (?, ?, ?, ?)                
                """);
                st.setInt(1, state.getUserId());
                st.setString(2, task.getTask());
                st.setString(3, task.getAnswer());
                st.setInt(4, state.getTries());
                st.execute();
                st.close();
            } else {
                // Update
                PreparedStatement st = conn.prepareStatement("""
                UPDATE state
                SET
                  task=?,
                  answer=?,
                  tries=?
                WHERE
                  user_id=?                
                """);
                st.setString(1, task.getTask());
                st.setString(2, task.getAnswer());
                st.setInt(3, state.getTries());
                st.setInt(4, state.getUserId());
                st.execute();
                st.close();
            }
            //conn.commit();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Save to local storage.
        stateStorage.put(state.getUserId(), state);
    }
}
