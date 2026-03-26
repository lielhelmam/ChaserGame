package com.example.chasergame.services;

import com.example.chasergame.adapters.QuestionsAdapter;
import com.example.chasergame.models.Question;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IQuestionRepository {
    void getQuestionList(@NotNull DatabaseService.DatabaseCallback<List<QuestionsAdapter.Item>> callback);

    void deleteQuestion(@NotNull String key, @Nullable DatabaseService.DatabaseCallback<Void> callback);

    void updateQuestion(@NotNull String key, @NotNull Question question, @Nullable DatabaseService.DatabaseCallback<Void> callback);

    void addQuestion(@NotNull Question question, @Nullable DatabaseService.DatabaseCallback<Void> callback);

    void deleteQuestionAndReindex(@NotNull String deleteKey, @Nullable DatabaseService.DatabaseCallback<Void> callback);
}
