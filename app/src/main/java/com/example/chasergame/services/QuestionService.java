package com.example.chasergame.services;

import com.example.chasergame.adapters.QuestionsAdapter;
import com.example.chasergame.models.Question;

import java.util.List;

public class QuestionService {
    private final IQuestionRepository questionRepository;

    public QuestionService(IQuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public void getAllQuestions(DatabaseService.DatabaseCallback<List<QuestionsAdapter.Item>> callback) {
        questionRepository.getQuestionList(new DatabaseService.DatabaseCallback<List<QuestionsAdapter.Item>>() {
            @Override
            public void onCompleted(List<QuestionsAdapter.Item> items) {
                // Sort by numeric key
                items.sort((a, b) -> {
                    int ka = safeParseInt(a.key);
                    int kb = safeParseInt(b.key);
                    return Integer.compare(ka, kb);
                });
                callback.onCompleted(items);
            }

            @Override
            public void onFailed(Exception e) {
                callback.onFailed(e);
            }
        });
    }

    private int safeParseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    public void addQuestion(Question question, DatabaseService.DatabaseCallback<Void> callback) {
        questionRepository.addQuestion(question, callback);
    }

    public void deleteQuestion(String key, DatabaseService.DatabaseCallback<Void> callback) {
        questionRepository.deleteQuestionAndReindex(key, callback);
    }

    public void updateQuestion(String key, Question question, DatabaseService.DatabaseCallback<Void> callback) {
        questionRepository.updateQuestion(key, question, callback);
    }
}
