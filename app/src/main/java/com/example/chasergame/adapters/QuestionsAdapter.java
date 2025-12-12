package com.example.chasergame.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chasergame.R;
import com.example.chasergame.models.Question;

import java.util.ArrayList;
import java.util.List;

public class QuestionsAdapter extends RecyclerView.Adapter<QuestionsAdapter.VH> {

    public interface Listener {
        void onEditClicked(String key, Question question);
        void onDeleteClicked(String key);
    }

    private final Listener listener;

    // list shown in UI
    private final List<Item> visible = new ArrayList<>();
    // master list for filtering
    private final List<Item> all = new ArrayList<>();

    public QuestionsAdapter(Listener listener) {
        this.listener = listener;
    }

    public static class Item {
        public final String key;     // "0", "1", ...
        public final Question value; // Question object
        public Item(String key, Question value) {
            this.key = key;
            this.value = value;
        }
    }

    public void setItems(List<Item> items) {
        all.clear();
        all.addAll(items);

        visible.clear();
        visible.addAll(items);

        notifyDataSetChanged();
    }

    public void filter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();

        visible.clear();

        if (q.isEmpty()) {
            visible.addAll(all);
            notifyDataSetChanged();
            return;
        }

        boolean numeric = q.matches("\\d+");
        int wantedKeyIndex = -1;
        if (numeric) {
            // user types "1" => key "0"
            try {
                wantedKeyIndex = Integer.parseInt(q) - 1;
            } catch (Exception ignored) {
                wantedKeyIndex = -1;
            }
        }

        for (Item item : all) {
            String questionText = "";
            if (item.value != null && item.value.getQuestion() != null) {
                questionText = item.value.getQuestion().toLowerCase();
            }

            boolean matchText = questionText.contains(q);
            boolean matchNumber = numeric
                    && item.key != null
                    && item.key.equals(String.valueOf(wantedKeyIndex));

            if (matchText || matchNumber) {
                visible.add(item);
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_question, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Item item = visible.get(position);
        Question q = item.value;

        // Number: key "0" => #1
        String numberText = "#" + item.key;
        try {
            int num = Integer.parseInt(item.key) + 1;
            numberText = "#" + num;
        } catch (Exception ignored) { }

        h.tvNumber.setText(numberText);

        h.tvQuestion.setText(q != null && q.getQuestion() != null ? q.getQuestion() : "-");
        h.tvRight.setText("Right: " + (q != null && q.getRightAnswer() != null ? q.getRightAnswer() : "-"));

        List<String> wrongs = (q != null) ? q.getWrongAnswers() : null;
        h.tvWrong.setText("Wrong: " + joinWithComma(wrongs));

        h.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClicked(item.key, q);
        });

        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClicked(item.key);
        });
    }

    @Override
    public int getItemCount() {
        return visible.size();
    }

    private String joinWithComma(List<String> list) {
        if (list == null || list.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i);
            if (s == null) s = "";
            if (i > 0) sb.append(", ");
            sb.append(s);
        }
        return sb.toString();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvNumber, tvQuestion, tvRight, tvWrong;
        Button btnEdit, btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.tv_question_number);
            tvQuestion = itemView.findViewById(R.id.tv_question);
            tvRight = itemView.findViewById(R.id.tv_right_answer);
            tvWrong = itemView.findViewById(R.id.tv_wrong_answers);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
