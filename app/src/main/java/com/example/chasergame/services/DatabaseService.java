package com.example.chasergame.services;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.chasergame.models.Question;
import com.example.chasergame.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/// a service to interact with the Firebase Realtime Database.
/// this class is a singleton, use getInstance() to get an instance of this class
///
/// @see #getInstance()
/// @see FirebaseDatabase
public class DatabaseService {

    /// tag for logging
    ///
    /// @see Log
    private static final String TAG = "DatabaseService";

    /// paths for different data types in the database
    ///
    /// @see DatabaseService#readData(String)
    private static final String USERS_PATH = "users",
            QUESTIONS_PATH = "questions";
    /// the instance of this class
    ///
    /// @see #getInstance()
    private static DatabaseService instance;
    /// the reference to the database
    ///
    /// @see DatabaseReference
    /// @see FirebaseDatabase#getReference()
    private final DatabaseReference databaseReference;

    /// use getInstance() to get an instance of this class
    ///
    /// @see DatabaseService#getInstance()
    private DatabaseService() {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
    }

    /// get an instance of this class
    ///
    /// @return an instance of this class
    /// @see DatabaseService
    public static DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    /// write data to the database at a specific path
    ///
    /// @param path     the path to write the data to
    /// @param data     the data to write (can be any object, but must be serializable, i.e. must have a default constructor and all fields must have getters and setters)
    /// @param callback the callback to call when the operation is completed
    /// @see DatabaseCallback
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


    // region private generic methods
    // to write and read data from the database

    /// remove data from the database at a specific path
    ///
    /// @param path     the path to remove the data from
    /// @param callback the callback to call when the operation is completed
    /// @see DatabaseCallback
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

    /// read data from the database at a specific path
    ///
    /// @param path the path to read the data from
    /// @return a DatabaseReference object to read the data from
    /// @see DatabaseReference

    private DatabaseReference readData(@NotNull final String path) {
        return databaseReference.child(path);
    }

    /// get data from the database at a specific path
    ///
    /// @param path     the path to get the data from
    /// @param callback the callback to call when the operation is completed
    /// @see DatabaseCallback
    /// @see Class
    private <T> void getData(@NotNull final String path, @NotNull final DatabaseCallback<T> callback) {
        readData(path).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error getting data", task.getException());
                callback.onFailed(task.getException());
                return;
            }
            T data = task.getResult().getValue((Class<T>) User.class);
            callback.onCompleted(data);
        });
    }

    /// get a list of data from the database at a specific path
    ///
    /// @param path     the path to get the data from
    /// @param clazz    the class of the objects to return
    /// @param callback the callback to call when the operation is completed
    private <T> void getDataList(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull final DatabaseCallback<List<T>> callback) {
        readData(path).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error getting data", task.getException());
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

    /// generate a new id for a new object in the database
    ///
    /// @param path the path to generate the id for
    /// @return a new id for the object
    /// @see String
    /// @see DatabaseReference#push()

    private String generateNewId(@NotNull final String path) {
        return databaseReference.child(path).push().getKey();
    }

    /// generate a new id for a new user in the database
    ///
    /// @return a new id for the user
    /// @see #generateNewId(String)
    /// @see User
    public String generateUserId() {
        return generateNewId(USERS_PATH);
    }

    // endregion of private methods for reading and writing data

    // public methods to interact with the database

    // region User Section

    /// create a new user in the database
    ///
    /// @param user     the user object to create
    /// @param callback the callback to call when the operation is completed
    ///                                                              the callback will receive void
    ///                                                            if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see User
    public void createNewUser(@NotNull final User user, @Nullable final DatabaseCallback<Void> callback) {
        writeData(USERS_PATH + "/" + user.getId(), user, callback);
    }

    /// get a user from the database
    ///
    /// @param uid      the id of the user to get
    /// @param callback the callback to call when the operation is completed
    ///                                                               the callback will receive the user object
    ///                                                             if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see User
    public void getUser(@NotNull final String uid, @NotNull final DatabaseCallback<User> callback) {
        getData(USERS_PATH + "/" + uid, callback);
    }

    /// get all the users from the database
    ///
    /// @param callback the callback to call when the operation is completed
    ///                                                              the callback will receive a list of user objects
    ///                                                            if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see List
    /// @see User
    public void getUserList(@NotNull final DatabaseCallback<List<User>> callback) {
        getDataList(USERS_PATH, User.class, callback);
    }

    /// delete a user from the database
    ///
    /// @param uid      the user id to delete
    /// @param callback the callback to call when the operation is completed
    public void deleteUserById(@NotNull final String uid, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(USERS_PATH + "/" + uid, callback);
    }

    /// get a user by email and password
    ///
    /// @param username the email of the user
    /// @param password the password of the user
    /// @param callback the callback to call when the operation is completed
    ///                                                            the callback will receive the user object
    ///                                                          if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see User
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

    /// check if an email already exists in the database
    ///
    /// @param email    the email to check
    /// @param callback the callback to call when the operation is completed
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

    public void updateUser(@NotNull final User user, @Nullable final DatabaseCallback<Void> callback) {
        writeData(USERS_PATH + "/" + user.getId(), user, callback);
    }

    public void updateUserWins(@NotNull final String userId, @NotNull final String winType, @Nullable final DatabaseCallback<Void> callback) {
        DatabaseReference userWinRef = readData(USERS_PATH + "/" + userId + "/" + winType);
        userWinRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Long currentValue = currentData.getValue(Long.class);
                if (currentValue == null) {
                    currentData.setValue(1L); // Use Long
                } else {
                    currentData.setValue(currentValue + 1);
                }
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (callback == null) return;
                if (error != null) {
                    callback.onFailed(error.toException());
                } else {
                    callback.onCompleted(null);
                }
            }
        });
    }

    //endregion

    //region Question Section

    /// get all the questions from the database
    ///
    /// @param callback the callback to call when the operation is completed
    ///                                                              the callback will receive a list of user objects
    ///                                                            if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see List
    /// @see Question
    public void getQuestionList(@NotNull final DatabaseCallback<List<Question>> callback) {
        getDataList(QUESTIONS_PATH, Question.class, callback);
    }

    public void deleteQuestion(@NonNull String key, @Nullable DatabaseCallback<Void> callback) {
        deleteData(QUESTIONS_PATH + "/" + key, callback);
    }

    public void updateQuestion(@NonNull String key, @NonNull Question question, @Nullable DatabaseCallback<Void> callback) {
        writeData(QUESTIONS_PATH + "/" + key, question, callback);
    }

    public void addQuestion(@NotNull Question question, @Nullable DatabaseCallback<Void> callback) {
        String key = generateNewId(QUESTIONS_PATH);
        writeData(QUESTIONS_PATH + "/" + key, question, callback);
    }

    public void deleteQuestionAndReindex(@NonNull String deleteKey, @Nullable DatabaseCallback<Void> callback) {

        readData(QUESTIONS_PATH).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {

                // collect all questions sorted by numeric key
                ArrayList<Question> list = new ArrayList<>();

                for (MutableData child : currentData.getChildren()) {
                    String k = child.getKey();
                    if (k == null) continue;
                    if (k.equals(deleteKey)) continue; // skip deleted item

                    Question q = child.getValue(Question.class);
                    if (q != null) list.add(q);
                }

                // wipe node and rewrite as 0..n-1
                currentData.setValue(null);
                for (int i = 0; i < list.size(); i++) {
                    currentData.child(String.valueOf(i)).setValue(list.get(i));
                }

                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot dataSnapshot) {
                if (error != null) {
                    if (callback != null) callback.onFailed(error.toException());
                } else {
                    if (callback != null) callback.onCompleted(null);
                }
            }
        });
    }

    //endregion


    /// callback interface for database operations
    ///
    /// @param <T> the type of the object to return
    /// @see DatabaseCallback#onCompleted(Object)
    /// @see DatabaseCallback#onFailed(Exception)
    public interface DatabaseCallback<T> {
        /// called when the operation is completed successfully
        void onCompleted(T object);

        /// called when the operation fails with an exception
        void onFailed(Exception e);
    }
}