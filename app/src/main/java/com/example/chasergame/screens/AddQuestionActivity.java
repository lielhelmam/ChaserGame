package com.example.chasergame.screens;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chasergame.R;
import com.example.chasergame.models.Question;
import com.example.chasergame.services.DatabaseService;

import java.util.ArrayList;
import java.util.Arrays;

public class AddQuestionActivity extends BaseActivity {

    private EditText etQuestion, etRightAnswer, etWrong1, etWrong2;
    private Button btnSaveQuestion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_add_question);

        // If your root view id is NOT "main", change it to the correct id from XML
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ✅ bind views
        etQuestion = findViewById(R.id.etQuestion);
        etRightAnswer = findViewById(R.id.etRightAnswer);
        etWrong1 = findViewById(R.id.etWrong1);
        etWrong2 = findViewById(R.id.etWrong2);
        btnSaveQuestion = findViewById(R.id.btnSaveQuestion);

        DatabaseService db = DatabaseService.getInstance();

        btnSaveQuestion.setOnClickListener(v -> {

            String q = etQuestion.getText().toString().trim();
            String right = etRightAnswer.getText().toString().trim();
            String w1 = etWrong1.getText().toString().trim();
            String w2 = etWrong2.getText().toString().trim();

            if (q.isEmpty() || right.isEmpty() || w1.isEmpty() || w2.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ assuming your fixed Question constructor is:
            // Question(String question, String rightAnswer, ArrayList<String> wrongAnswers)
            Question question = new Question(
                    q,
                    right,
                    new ArrayList<>(Arrays.asList(w1, w2))
            );

            db.addQuestion(question, new DatabaseService.DatabaseCallback<Void>() {
                @Override
                public void onCompleted(Void object) {
                    Toast.makeText(AddQuestionActivity.this, "Question saved", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailed(Exception e) {
                    Toast.makeText(AddQuestionActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}