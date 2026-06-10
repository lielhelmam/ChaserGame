package com.example.chasergame.screens;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chasergame.R;
import com.example.chasergame.models.SongData;
import com.example.chasergame.services.DatabaseService;
import com.google.firebase.database.DataSnapshot;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdminActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.AdminPage), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupButtons();
    }

    private void setupButtons() {
        findViewById(R.id.btnAddQuestion).setOnClickListener(v ->
                navigateTo(AddQuestionActivity.class, false));

        findViewById(R.id.btn_admin_questions_list).setOnClickListener(v ->
                navigateTo(QuestionsListActivity.class, false));

        findViewById(R.id.btn_admin_gotouserlist).setOnClickListener(v ->
                navigateTo(UsersListActivity.class, false));

        findViewById(R.id.btn_admin_manage_songs).setOnClickListener(v ->
                navigateTo(ManageSongsActivity.class, false));

        findViewById(R.id.btn_admin_songs_list).setOnClickListener(v ->
                navigateTo(SongsListActivity.class, false));

        findViewById(R.id.btn_admin_auto_import).setOnClickListener(v -> autoImportSongs());
    }

    private void autoImportSongs() {
        databaseService.getSongsSnapshot(new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(List<DataSnapshot> snapshots) {
                Set<String> existingResNames = new HashSet<>();
                for (DataSnapshot snap : snapshots) {
                    SongData s = snap.getValue(SongData.class);
                    if (s != null && s.getResName() != null) {
                        existingResNames.add(s.getResName());
                    }
                }

                int importedCount = 0;
                Field[] fields = com.example.chasergame.R.raw.class.getFields();
                for (Field field : fields) {
                    String resName = field.getName();
                    
                    // Skip internal resources if any
                    if (existingResNames.contains(resName)) continue;

                    // Create new song entry
                    SongData newSong = new SongData();
                    newSong.setResName(resName);
                    newSong.setName(prettifyName(resName));
                    newSong.setDifficulty("Medium");
                    newSong.setBpm(120);
                    newSong.setHpDrain(1);
                    newSong.setHpGain(1);

                    databaseService.addSong(newSong, null);
                    importedCount++;
                }

                Toast.makeText(AdminActivity.this, "Imported " + importedCount + " new songs!", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(AdminActivity.this, "Import failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String prettifyName(String rawName) {
        if (rawName == null || rawName.isEmpty()) return "Unknown Song";
        
        // Remove 's' prefix if it was added for numeric starts
        String name = rawName;
        if (name.startsWith("s") && name.length() > 1 && Character.isDigit(name.charAt(1))) {
            name = name.substring(1);
        }

        // Capitalize words
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (capitalizeNext && Character.isLetter(c)) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
            if (Character.isDigit(c)) capitalizeNext = true;
        }
        return sb.toString();
    }
}
