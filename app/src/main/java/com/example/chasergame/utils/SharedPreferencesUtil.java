package com.example.chasergame.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.example.chasergame.models.User;
import com.google.gson.Gson;

/// Utility class for shared preferences operations
/// Contains methods for saving and retrieving data from shared preferences
/// Also contains methods for clearing and removing data from shared preferences
///
/// @see SharedPreferences
public class SharedPreferencesUtil {

    /// The name of the shared preferences file
    ///
    /// @see Context#getSharedPreferences(String, int)
    private static final String PREF_NAME = "com.example.chasergame.PREFERENCE_FILE_KEY";

    /// Save a string to shared preferences
    ///
    /// @param context The context to use
    /// @param key     The key to save the string with
    /// @param value   The string to save
    /// @see SharedPreferences.Editor#putString(String, String)
    private static void saveString(Context context, String key, String value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /// Get a string from shared preferences
    ///
    /// @param context The context to use
    /// @param key     The key to get the string with
    /// @return The string value stored in shared preferences
    /// @see SharedPreferences#getString(String, String)
    private static String getString(Context context, String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, null);
    }

    /// Save an integer to shared preferences
    ///
    /// @param context The context to use
    /// @param key     The key to save the integer with
    /// @param value   The integer to save
    /// @see SharedPreferences.Editor#putInt(String, int)
    private static void saveInt(Context context, String key, int value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    /// Get an integer from shared preferences
    ///
    /// @param context      The context to use
    /// @param key          The key to get the integer with
    /// @param defaultValue The default value to return if the key is not found
    /// @return The integer value stored in shared preferences
    /// @see SharedPreferences#getInt(String, int)
    private static int getInt(Context context, String key, int defaultValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(key, defaultValue);
    }

    // Add more methods for other data types as needed

    /// Clear all data from shared preferences
    ///
    /// @param context The context to use
    /// @see SharedPreferences.Editor#clear()
    public static void clear(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    /// Remove a specific key from shared preferences
    ///
    /// @param context The context to use
    /// @see SharedPreferences.Editor#remove(String)
    private static void remove(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("user");
        editor.apply();
    }

    /// Check if a key exists in shared preferences
    ///
    /// @param context The context to use
    /// @return true if the key exists, false otherwise
    /// @see SharedPreferences#contains(String)
    private static boolean contains(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.contains("user");
    }

    private static <T> void saveObject(Context context, T object) {
        Gson gson = new Gson();
        String json = gson.toJson(object);
        saveString(context, "user", json);
    }

    private static <T> T getObject(Context context) {
        String json = getString(context, "user");
        if (json == null) {
            return null;
        }
        Gson gson = new Gson();
        return gson.fromJson(json, (Class<T>) User.class);
    }

    // Add more utility methods as needed

    /// Save a user object to shared preferences
    ///
    /// @param context The context to use
    /// @param user    The user object to save
    /// @see User
    public static void saveUser(Context context, User user) {
        saveObject(context, user);
    }

    /// Get the user object from shared preferences
    ///
    /// @param context The context to use
    /// @return The user object stored in shared preferences
    /// @see User
    /// @see #isUserLoggedIn(Context)
    public static User getUser(Context context) {
        if (!isUserLoggedIn(context)) {
            return null;
        }
        return getObject(context);
    }

    /// Sign out the user by removing user data from shared preferences
    ///
    /// @param context The context to use
    public static void signOutUser(Context context) {
        remove(context);
    }

    /// Check if a user is logged in by checking if the user id is present in shared preferences
    ///
    /// @param context The context to use
    /// @return true if the user is logged in, false otherwise
    /// @see #contains(Context)
    public static boolean isUserLoggedIn(Context context) {
        return contains(context);
    }

    /// Get the user id of the logged in user
    ///
    /// @param context The context to use
    /// @return The user id of the logged in user, or null if no user is logged in
    @Nullable
    public static String getUserId(Context context) {
        User user = getUser(context);
        if (user != null) {
            return user.getId();
        }
        return null;
    }


}