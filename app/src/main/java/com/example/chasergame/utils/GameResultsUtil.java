package com.example.chasergame.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.chasergame.models.GameResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GameResultsUtil {

    private static final String PREF_NAME = "com.example.chasergame.GAME_RESULTS_PREF";
    private static final String KEY_GAME_RESULTS = "game_results";

    public static void saveGameResult(Context context, GameResult gameResult) {
        List<GameResult> gameResults = getGameResults(context);
        if (gameResults == null) {
            gameResults = new ArrayList<>();
        }
        gameResults.add(gameResult);

        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(gameResults);
        editor.putString(KEY_GAME_RESULTS, json);
        editor.apply();
    }

    public static List<GameResult> getGameResults(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(KEY_GAME_RESULTS, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<GameResult>>() {}.getType();
        return gson.fromJson(json, type);
    }
}