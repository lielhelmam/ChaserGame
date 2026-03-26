package com.example.chasergame.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.chasergame.adapters.QuestionsAdapter;
import com.example.chasergame.adapters.SongsAdminAdapter;
import com.example.chasergame.models.Question;
import com.example.chasergame.models.SongData;
import com.example.chasergame.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class DatabaseService implements IUserRepository, IQuestionRepository, ISongRepository {

    private static final String TAG = "DatabaseService";

    private static final String USERS_PATH = "users",
            QUESTIONS_PATH = "questions",
            SONGS_PATH = "rhythm_songs";

    private static DatabaseService instance;
    private final DatabaseReference databaseReference;

    private DatabaseService() {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
    }

    public static DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    private void writeData(@NotNull final String path, @NotNull final Object data, final @Nullable DatabaseCallback<Void> callback) {
        readData(path).setValue(data, (error, ref) -> {
            if (error != null) {
                if (callback == null) return;
                callback.onFailed(error.toException());
            } else {
                if (callback == null) return;
                callback.onCompleted(null);
            }
        });
    }

    private void deleteData(@NotNull final String path, @Nullable final DatabaseCallback<Void> callback) {
        readData(path).removeValue((error, ref) -> {
            if (error != null) {
                if (callback == null) return;
                callback.onFailed(error.toException());
            } else {
                if (callback == null) return;
                callback.onCompleted(null);
            }
        });
    }

    private DatabaseReference readData(@NotNull final String path) {
        return databaseReference.child(path);
    }

    private <T> void getDataList(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull final DatabaseCallback<List<T>> callback) {
        readData(path).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onFailed(task.getException());
                return;
            }
            List<T> tList = new ArrayList<>();
            task.getResult().getChildren().forEach(dataSnapshot -> {
                T t = dataSnapshot.getValue(clazz);
                tList.add(t);
            });
            callback.onCompleted(tList);
        });
    }

    private String generateNewId(@NotNull final String path) {
        return databaseReference.child(path).push().getKey();
    }

    @Override
    public String generateUserId() {
        return generateNewId(USERS_PATH);
    }

    // region User Section
    @Override
    public void createNewUser(@NotNull final User user, @Nullable final DatabaseCallback<Void> callback) {
        writeData(USERS_PATH + "/" + user.getId(), user, callback);
    }

    @Override
    public void getUser(@NotNull final String uid, @NotNull final DatabaseCallback<User> callback) {
        readData(USERS_PATH + "/" + uid).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onFailed(task.getException());
                return;
            }
            User user = task.getResult().getValue(User.class);
            callback.onCompleted(user);
        });
    }

    @Override
    public void getUserList(@NotNull final DatabaseCallback<List<User>> callback) {
        getDataList(USERS_PATH, User.class, callback);
    }

    @Override
    public void deleteUserById(@NotNull final String uid, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(USERS_PATH + "/" + uid, callback);
    }

    @Override
    public void getUserByUsernameAndPassword(@NotNull final String username, @NotNull final String password, @NotNull final DatabaseCallback<User> callback) {
        getUserList(new DatabaseCallback<>() {
            @Override
            public void onCompleted(List<User> users) {
                for (User user : users) {
                    if (Objects.equals(user.getUsername(), username) && Objects.equals(user.getPassword(), password)) {
                        callback.onCompleted(user);
                        return;
                    }
                }
                callback.onCompleted(null);
            }

            @Override
            public void onFailed(Exception e) {
                callback.onFailed(e);
            }
        });
    }

    @Override
    public void checkIfEmailExists(@NotNull final String email, @NotNull final DatabaseCallback<Boolean> callback) {
        getUserList(new DatabaseCallback<>() {
            @Override
            public void onCompleted(List<User> users) {
                for (User user : users) {
                    if (Objects.equals(user.getEmail(), email)) {
                        callback.onCompleted(true);
                        return;
                    }
                }
                callback.onCompleted(false);
            }

            @Override
            public void onFailed(Exception e) {
                callback.onFailed(e);
            }
        });
    }

    @Override
    public void updateUser(@NotNull final User user, @Nullable final DatabaseCallback<Void> callback) {
        writeData(USERS_PATH + "/" + user.getId(), user, callback);
    }

    @Override
    public void updateUserWins(@NotNull final String userId, @NotNull final String winType, @Nullable final DatabaseCallback<Void> callback) {
        DatabaseReference userWinRef = readData(USERS_PATH + "/" + userId + "/" + winType);
        userWinRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Long currentValue = currentData.getValue(Long.class);
                currentData.setValue(currentValue == null ? 1L : currentValue + 1);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (callback != null) {
                    if (error != null) callback.onFailed(error.toException());
                    else callback.onCompleted(null);
                }
            }
        });
    }
    //endregion

    //region Question Section
    @Override
    public void getQuestionList(@NotNull final DatabaseCallback<List<QuestionsAdapter.Item>> callback) {
        readData(QUESTIONS_PATH).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<QuestionsAdapter.Item> items = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String key = child.getKey();
                    Question q = child.getValue(Question.class);
                    if (q != null) items.add(new QuestionsAdapter.Item(key, q));
                }
                callback.onCompleted(items);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailed(error.toException());
            }
        });
    }

    @Override
    public void deleteQuestion(@NonNull String key, @Nullable DatabaseCallback<Void> callback) {
        deleteData(QUESTIONS_PATH + "/" + key, callback);
    }

    @Override
    public void updateQuestion(@NonNull String key, @NonNull Question question, @Nullable DatabaseCallback<Void> callback) {
        writeData(QUESTIONS_PATH + "/" + key, question, callback);
    }

    @Override
    public void addQuestion(@NotNull Question question, @Nullable DatabaseCallback<Void> callback) {
        String key = generateNewId(QUESTIONS_PATH);
        writeData(QUESTIONS_PATH + "/" + key, question, callback);
    }

    @Override
    public void deleteQuestionAndReindex(@NonNull String deleteKey, @Nullable DatabaseCallback<Void> callback) {
        readData(QUESTIONS_PATH).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                ArrayList<Question> list = new ArrayList<>();
                for (MutableData child : currentData.getChildren()) {
                    String k = child.getKey();
                    if (k != null && !k.equals(deleteKey)) {
                        Question q = child.getValue(Question.class);
                        if (q != null) list.add(q);
                    }
                }
                currentData.setValue(null);
                for (int i = 0; i < list.size(); i++) {
                    currentData.child(String.valueOf(i)).setValue(list.get(i));
                }
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot dataSnapshot) {
                if (callback != null) {
                    if (error != null) callback.onFailed(error.toException());
                    else callback.onCompleted(null);
                }
            }
        });
    }
    //endregion

    // region Song Section
    @Override
    public void getSongList(@NotNull DatabaseCallback<List<SongData>> callback) {
        getDataList(SONGS_PATH, SongData.class, callback);
    }

    public void getSongListWithKeys(@NotNull DatabaseCallback<List<SongsAdminAdapter.Item>> callback) {
        readData(SONGS_PATH).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<SongsAdminAdapter.Item> items = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String key = child.getKey();
                    SongData song = child.getValue(SongData.class);
                    if (key != null && song != null)
                        items.add(new SongsAdminAdapter.Item(key, song));
                }
                callback.onCompleted(items);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailed(error.toException());
            }
        });
    }

    @Override
    public void addSong(@NotNull SongData song, @Nullable DatabaseCallback<Void> callback) {
        String songId = generateNewId(SONGS_PATH);
        writeData(SONGS_PATH + "/" + songId, song, callback);
    }

    @Override
    public void deleteSong(@NotNull String songId, @Nullable DatabaseCallback<Void> callback) {
        deleteData(SONGS_PATH + "/" + songId, callback);
    }

    @Override
    public void updateSong(@NotNull String songId, @NotNull SongData song, @Nullable DatabaseCallback<Void> callback) {
        writeData(SONGS_PATH + "/" + songId, song, callback);
    }
    // endregion

    public interface DatabaseCallback<T> {
        void onCompleted(T object);

        void onFailed(Exception e);
    }
}
