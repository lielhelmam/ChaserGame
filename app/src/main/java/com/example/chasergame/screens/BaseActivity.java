package com.example.chasergame.screens;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.chasergame.R;
import com.example.chasergame.services.DatabaseService;

public class BaseActivity extends AppCompatActivity {

    protected DatabaseService databaseService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /// get the instance of the database service
        databaseService = DatabaseService.getInstance();
    }
}
