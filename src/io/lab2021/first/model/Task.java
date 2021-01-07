package io.lab2021.first.model;

// See https://github.com/tochka-lab-2021/math-quiz-bot/blob/main/task.py

import lombok.Data;

@Data
public class Task {
    private String task;
    private String answer;
}
