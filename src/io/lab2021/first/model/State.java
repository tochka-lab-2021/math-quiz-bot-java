package io.lab2021.first.model;

// See https://github.com/tochka-lab-2021/math-quiz-bot/blob/main/state.py

import lombok.Data;

@Data
public class State {
    private Integer userId;
    private Task task;
    private Integer tries;
    private Integer messageWithInlineKeyboardId;
    private Boolean newState;

    public State() {
        newState = false;
    }
}
