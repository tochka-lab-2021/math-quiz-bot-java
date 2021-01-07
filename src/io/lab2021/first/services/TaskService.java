package io.lab2021.first.services;

import io.lab2021.first.model.Task;
import java.util.Random;

public class TaskService {
    public TaskService() {
    }

    // See https://github.com/tochka-lab-2021/math-quiz-bot/blob/main/bot.py#L12-L21
    public Task genTask() {
        Random rand = new Random();
        int a = 1 + rand.nextInt(9); // 1 - 9
        int b = 1 + rand.nextInt(9); // 1 - 9

        Task task = new Task();
        task.setTask(String.format("%dx%d", a, b));
        task.setAnswer(String.format("%d", a*b));

        return task;
    }
}
