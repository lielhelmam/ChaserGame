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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class QuestionsListActivity extends BaseActivity {

    private RecyclerView rv;
    private SearchView searchView;
    private QuestionsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rv = findViewById(R.id.rv_questions_list);
        searchView = findViewById(R.id.searchView);

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

        loadQuestions();
    }

    private void loadQuestions() {
        FirebaseDatabase.getInstance()
                .getReference("questions")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<QuestionsAdapter.Item> items = new ArrayList<>();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            String key = child.getKey(); // "0","1",...
                            Question q = child.getValue(Question.class);
                            items.add(new QuestionsAdapter.Item(key, q));
                        }

                        // ✅ sort by numeric key so list order is correct
                        Collections.sort(items, new Comparator<QuestionsAdapter.Item>() {
                            @Override
                            public int compare(QuestionsAdapter.Item a, QuestionsAdapter.Item b) {
                                int ka = safeParseInt(a.key, Integer.MAX_VALUE);
                                int kb = safeParseInt(b.key, Integer.MAX_VALUE);
                                return Integer.compare(ka, kb);
                            }
                        });

                        adapter.setItems(items);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(QuestionsListActivity.this, "Load failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private int safeParseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return fallback;
        }
    }

    private void confirmDelete(String key) {
        new AlertDialog.Builder(this)
                .setTitle("Delete question")
                .setMessage("Are you sure?")
                .setPositiveButton("Delete", (d, w) -> {

                    // ✅ Delete + reindex so DB becomes 0..n-1 again
                    databaseService.deleteQuestionAndReindex(key, new DatabaseService.DatabaseCallback<Void>() {
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
                            new ArrayList<>(java.util.Arrays.asList(
                                    etW1.getText().toString().trim(),
                                    etW2.getText().toString().trim()
                            ))
                    );

                    databaseService.updateQuestion(key, updated, new DatabaseService.DatabaseCallback<Void>() {
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
