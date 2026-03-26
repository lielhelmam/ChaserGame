package com.example.chasergame.screens;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;
import com.example.chasergame.adapters.QuestionsAdapter;
import com.example.chasergame.models.Question;
import com.example.chasergame.services.DatabaseService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuestionsListActivity extends BaseActivity {

    private QuestionsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        setupRecyclerView();
        setupSearchView();
        loadQuestions();
    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rv_questions_list);
        adapter = new QuestionsAdapter(new QuestionsAdapter.Listener() {
            @Override
            public void onEditClicked(String key, Question question) {
                showEditDialog(key, question);
            }

            @Override
            public void onDeleteClicked(String key) {
                confirmDelete(key);
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
    }

    private void setupSearchView() {
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });
    }

    private void loadQuestions() {
        questionService.getAllQuestions(new DatabaseService.DatabaseCallback<List<QuestionsAdapter.Item>>() {
            @Override
            public void onCompleted(List<QuestionsAdapter.Item> items) {
                adapter.setItems(items);
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(QuestionsListActivity.this, "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDelete(String key) {
        new AlertDialog.Builder(this)
                .setTitle("Delete question")
                .setMessage("Are you sure?")
                .setPositiveButton("Delete", (d, w) -> {
                    questionService.deleteQuestion(key, new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(Void object) {
                            Toast.makeText(QuestionsListActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                            loadQuestions();
                        }

                        @Override
                        public void onFailed(Exception e) {
                            Toast.makeText(QuestionsListActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditDialog(String key, Question q) {
        android.view.View v = getLayoutInflater().inflate(R.layout.dialog_edit_question, null);

        android.widget.EditText etQ = v.findViewById(R.id.et_edit_question);
        android.widget.EditText etR = v.findViewById(R.id.et_edit_right);
        android.widget.EditText etW1 = v.findViewById(R.id.et_edit_wrong1);
        android.widget.EditText etW2 = v.findViewById(R.id.et_edit_wrong2);

        etQ.setText(q.getQuestion());
        etR.setText(q.getRightAnswer());
        if (q.getWrongAnswers() != null && q.getWrongAnswers().size() > 0)
            etW1.setText(q.getWrongAnswers().get(0));
        if (q.getWrongAnswers() != null && q.getWrongAnswers().size() > 1)
            etW2.setText(q.getWrongAnswers().get(1));

        new AlertDialog.Builder(this)
                .setTitle("Edit question")
                .setView(v)
                .setPositiveButton("Save", (d, w) -> {
                    Question updated = new Question(
                            etQ.getText().toString().trim(),
                            etR.getText().toString().trim(),
                            new ArrayList<>(Arrays.asList(
                                    etW1.getText().toString().trim(),
                                    etW2.getText().toString().trim()
                            ))
                    );

                    questionService.updateQuestion(key, updated, new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(Void object) {
                            Toast.makeText(QuestionsListActivity.this, "Updated", Toast.LENGTH_SHORT).show();
                            loadQuestions();
                        }

                        @Override
                        public void onFailed(Exception e) {
                            Toast.makeText(QuestionsListActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
