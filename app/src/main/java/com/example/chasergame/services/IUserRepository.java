package com.example.chasergame.services;

import com.example.chasergame.models.User;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IUserRepository {
    String generateUserId();

    void createNewUser(@NotNull User user, @Nullable DatabaseService.DatabaseCallback<Void> callback);

    void getUser(@NotNull String uid, @NotNull DatabaseService.DatabaseCallback<User> callback);

    void getUserList(@NotNull DatabaseService.DatabaseCallback<List<User>> callback);

    void deleteUserById(@NotNull String uid, @Nullable DatabaseService.DatabaseCallback<Void> callback);

    void getUserByUsernameAndPassword(@NotNull String username, @NotNull String password, @NotNull DatabaseService.DatabaseCallback<User> callback);

    void checkIfEmailExists(@NotNull String email, @NotNull DatabaseService.DatabaseCallback<Boolean> callback);

    void updateUser(@NotNull User user, @Nullable DatabaseService.DatabaseCallback<Void> callback);

    void updateUserWins(@NotNull String userId, @NotNull String winType, @Nullable DatabaseService.DatabaseCallback<Void> callback);
}
