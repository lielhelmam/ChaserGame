package com.example.chasergame.services;

import android.content.Context;
import android.content.Intent;

import com.example.chasergame.models.User;
import com.example.chasergame.screens.AdminActivity;
import com.example.chasergame.screens.MainActivity;
import com.example.chasergame.utils.SharedPreferencesUtil;

import org.jetbrains.annotations.NotNull;

public class AuthService {
    private final IUserRepository userRepository;
    private final Context context;

    /**
     * Constructor for AuthService.
     *
     * @param context        The application context.
     * @param userRepository The repository to handle user data operations.
     */
    public AuthService(Context context, IUserRepository userRepository) {
        this.context = context.getApplicationContext();
        this.userRepository = userRepository;
    }

    /**
     * Checks if a user is currently logged into the application.
     *
     * @return True if a user is logged in, false otherwise.
     */
    public boolean isUserLoggedIn() {
        return SharedPreferencesUtil.isUserLoggedIn(context);
    }

    /**
     * Retrieves the currently logged-in user from local storage.
     *
     * @return The current User object, or null if no user is logged in.
     */
    public User getCurrentUser() {
        return SharedPreferencesUtil.getUser(context);
    }

    /**
     * Syncs the user data by saving the provided user object to local storage.
     *
     * @param user The User object to sync.
     */
    public void syncUser(User user) {
        if (user != null) {
            SharedPreferencesUtil.saveUser(context, user);
        }
    }

    /**
     * Attempts to log in a user with the provided username and password.
     * Updates local storage with the user data upon successful login.
     *
     * @param username The username for login.
     * @param password The password for login.
     * @param callback Callback to handle the result of the login attempt.
     */
    public void login(@NotNull String username, @NotNull String password, @NotNull DatabaseService.DatabaseCallback<User> callback) {
        userRepository.getUserByUsernameAndPassword(username, password, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                if (user != null) {
                    syncUser(user);
                }
                callback.onCompleted(user);
            }

            @Override
            public void onFailed(Exception e) {
                callback.onFailed(e);
            }
        });
    }

    /**
     * Logs out the current user and clears their data from local storage.
     */
    public void logout() {
        SharedPreferencesUtil.signOutUser(context);
    }

    /**
     * Registers a new user after checking if the email already exists.
     *
     * @param user     The User object containing registration details.
     * @param callback Callback to handle the result of the registration.
     */
    public void register(@NotNull User user, @NotNull DatabaseService.DatabaseCallback<Void> callback) {
        userRepository.checkIfEmailExists(user.getEmail(), new DatabaseService.DatabaseCallback<Boolean>() {
            @Override
            public void onCompleted(Boolean exists) {
                if (exists) {
                    callback.onFailed(new Exception("Email already exists"));
                } else {
                    userRepository.createNewUser(user, callback);
                }
            }

            @Override
            public void onFailed(Exception e) {
                callback.onFailed(e);
            }
        });
    }

    /**
     * Updates the profile details of the given user.
     *
     * @param user     The current User object.
     * @param name     The new username.
     * @param email    The new email.
     * @param pass     The new password.
     * @param callback Callback to handle the result of the update.
     */
    public void updateProfile(@NotNull User user, String name, String email, String pass, @NotNull DatabaseService.DatabaseCallback<Void> callback) {
        user.setUsername(name);
        user.setEmail(email);
        user.setPassword(pass);
        userRepository.updateUser(user, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void unused) {
                syncUser(user);
                callback.onCompleted(unused);
            }

            @Override
            public void onFailed(Exception e) {
                callback.onFailed(e);
            }
        });
    }

    /**
     * Determines the next activity intent based on the user's role (Admin or Regular User).
     *
     * @return An Intent targeting the appropriate activity, or null if no user is logged in.
     */
    public Intent getNextActivityIntent() {
        User user = getCurrentUser();
        if (user == null) return null;
        Class<?> target = user.isAdmin() ? AdminActivity.class : MainActivity.class;
        Intent intent = new Intent(context, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }
}
