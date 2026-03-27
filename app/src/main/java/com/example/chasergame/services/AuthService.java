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

    public AuthService(Context context, IUserRepository userRepository) {
        this.context = context.getApplicationContext();
        this.userRepository = userRepository;
    }

    public boolean isUserLoggedIn() {
        return SharedPreferencesUtil.isUserLoggedIn(context);
    }

    public User getCurrentUser() {
        return SharedPreferencesUtil.getUser(context);
    }

    public void syncUser(User user) {
        if (user != null) {
            SharedPreferencesUtil.saveUser(context, user);
        }
    }

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

    public void logout() {
        SharedPreferencesUtil.signOutUser(context);
    }

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

    public Intent getNextActivityIntent() {
        User user = getCurrentUser();
        if (user == null) return null;
        Class<?> target = user.isAdmin() ? AdminActivity.class : MainActivity.class;
        Intent intent = new Intent(context, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }
}
