package com.example.chasergame.screens;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chasergame.R;
import com.example.chasergame.models.SongData;
import com.example.chasergame.services.DatabaseService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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

        findViewById(R.id.btn_admin_migrate_to_cloud).setOnClickListener(v -> migrateLocalSongsToCloud());

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
            // In our current strict naming, we don't have spaces, 
            // but let's make it look a bit better if it was camelCase or something
            if (Character.isDigit(c)) capitalizeNext = true;
        }
        return sb.toString();
    }

    private void migrateLocalSongsToCloud() {
        Log.i("AdminActivity", "Starting migration of local songs to cloud");
        databaseService.getSongsSnapshot(new DatabaseService.DatabaseCallback<>() {
            @Override
            public void onCompleted(List<DataSnapshot> snapshots) {
                List<DataSnapshot> toMigrate = new ArrayList<>();
                for (DataSnapshot snap : snapshots) {
                    SongData song = snap.getValue(SongData.class);
                    if (song != null && song.getAudioUrl() == null && song.getResName() != null) {
                        toMigrate.add(snap);
                    }
                }

                if (toMigrate.isEmpty()) {
                    Toast.makeText(AdminActivity.this, "No songs need migration", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(AdminActivity.this, "Starting migration of " + toMigrate.size() + " songs...", Toast.LENGTH_LONG).show();
                
                AtomicInteger completedCount = new AtomicInteger(0);
                for (DataSnapshot snap : toMigrate) {
                    SongData song = snap.getValue(SongData.class);
                    if (song != null) {
                        uploadResourceToCloud(snap.getKey(), song, (success) -> {
                            if (success) {
                                int current = completedCount.incrementAndGet();
                                if (current == toMigrate.size()) {
                                    Toast.makeText(AdminActivity.this, "Migration complete!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(AdminActivity.this, "Failed to fetch songs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadResourceToCloud(String songId, SongData song, java.util.function.Consumer<Boolean> callback) {
        String resName = song.getResName();
        int resId = getResources().getIdentifier(resName, "raw", getPackageName());
        
        if (resId == 0) {
            Log.e("AdminActivity", "Resource not found: " + resName);
            callback.accept(false);
            return;
        }

        Uri resUri = Uri.parse("android.resource://" + getPackageName() + "/" + resId);
        String fileName = "rhythm_songs/" + resName + "_" + System.currentTimeMillis() + ".mp3";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(fileName);

        Log.d("AdminActivity", "Uploading " + resName + " to " + fileName);
        
        storageRef.putFile(resUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        song.setAudioUrl(downloadUrl);
                        databaseService.updateSong(songId, song, new DatabaseService.DatabaseCallback<Void>() {
                            @Override
                            public void onCompleted(Void unused) {
                                Log.i("AdminActivity", "Successfully migrated " + resName);
                                callback.accept(true);
                            }

                            @Override
                            public void onFailed(Exception e) {
                                Log.e("AdminActivity", "Failed to update database for " + resName, e);
                                callback.accept(false);
                            }
                        });
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminActivity", "Failed to upload " + resName, e);
                    callback.accept(false);
                });
    }

    private String normalizeResName(String name) {
        if (name == null) return null;
        
        // Strip extension if present
        String result = name;
        if (result.toLowerCase().endsWith(".mp3")) result = result.substring(0, result.length() - 4);
        if (result.toLowerCase().endsWith(".wav")) result = result.substring(0, result.length() - 4);
        if (result.toLowerCase().endsWith(".ogg")) result = result.substring(0, result.length() - 4);

        // Remove EVERYTHING except lowercase letters and numbers
        result = result.toLowerCase().replaceAll("[^a-z0-9]", "");
        
        if (result.matches("^\\d.*")) {
            result = "s" + result;
        }
        return result;
    }
}
