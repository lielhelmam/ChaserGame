package com.example.chasergame.models;

import java.util.ArrayList;

public class Question {
    public String rightAnswer;
    public ArrayList<String> wrongAnswers;
    public String question;


    public Question(String rightAnswer, ArrayList<String> wrongAnswers, String question) {
        this.rightAnswer = rightAnswer;
        this.wrongAnswers = wrongAnswers;
        this.question = question;
    }

    public String getRightAnswer() {
        return rightAnswer;
    }

    public void setRightAnswer(String rightAnswer) {
        this.rightAnswer = rightAnswer;
    }

    public ArrayList<String> getWrongAnswers() {
        return wrongAnswers;
    }

    public void setWrongAnswers(ArrayList<String> wrongAnswers) {
        this.wrongAnswers = wrongAnswers;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
