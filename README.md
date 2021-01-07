# math-quiz-bot-java
[Math quiz](https://github.com/tochka-lab-2021/math-quiz-bot) Telegram bot implemented in Java.

## LoC comparison

```shell
$ wc -l math-quiz-bot/bot.py
228 math-quiz-bot/bot.py

$ find . -name \*.java -exec cat {} \; | wc -l
528

$  find . -name \*.java -exec wc -l {} \;
53 ./src/io/lab2021/first/Main.java
291 ./src/io/lab2021/first/handlers/MathQuizHandlers.java
11 ./src/io/lab2021/first/model/Task.java
18 ./src/io/lab2021/first/model/State.java
133 ./src/io/lab2021/first/services/StateService.java
22 ./src/io/lab2021/first/services/TaskService.java

```