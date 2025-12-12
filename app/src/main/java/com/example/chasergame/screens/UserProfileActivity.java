package com.example.chasergame.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chasergame.R;
import com.example.chasergame.models.User;
import com.example.chasergame.services.DatabaseService;

public class UserProfileActivity extends BaseActivity {
    private static final String TAG = "UserProfileActivity";

    private TextView tvName, tvEmail, tvPassword, tvAdmin;
    private ProgressBar progressBar;

    private String userId;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);


        setContentView(R.layout.activity_user_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        tvName = findViewById(R.id.tv_profile_name);
        tvEmail = findViewById(R.id.tv_profile_email);
        tvPassword = findViewById(R.id.tv_profile_password);
        tvAdmin = findViewById(R.id.tv_profile_admin);
        progressBar = findViewById(R.id.profile_progress);

        userId = getIntent().getStringExtra("USER_UID");
        if (userId == null || userId.trim().isEmpty()) {
            Log.e(TAG, "No USER_UID passed to activity");
            Toast.makeText(this, "No user specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadUser();

       // btnEdit.setOnClickListener(v -> {
            // Open edit screen — you can create one or use the same profile view in editable mode
         //   Intent i = new Intent(UserProfileActivity.this, EditUserActivity.class);
         //   i.putExtra("USER_UID", userId);
         //   startActivity(i);
       // });

      //  btnDelete.setOnClickListener(v -> {
            // confirm deletion
          //  new AlertDialog.Builder(this)
               //     .setTitle("Delete user")
               //     .setMessage("Are you sure you want to delete this user?")
                //    .setPositiveButton("Delete", (dialog, which) -> deleteUser())
              //      .setNegativeButton("Cancel", null)
               //     .show();
       // });
    }

    private void loadUser() {
        progressBar.setVisibility(View.VISIBLE);

        // Use DatabaseService instead of FirebaseDatabase directly
        databaseService.getUser(userId, new DatabaseService.DatabaseCallback<User>() {
            @Override
            public void onCompleted(User user) {
                progressBar.setVisibility(View.GONE);
                if (user == null) {
                    Toast.makeText(UserProfileActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                currentUser = user;
                showUserData(user);
            }

            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Failed to load user", e);
                Toast.makeText(UserProfileActivity.this, "Failed to load user", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showUserData(User user) {
        tvName.setText(user.getUsername() == null ? "-" : user.getUsername());
        tvEmail.setText(user.getEmail() == null ? "-" : user.getEmail());

        // WARNING: consider not showing passwords in plaintext in production
        tvPassword.setText(user.getPassword() == null ? "-" : user.getPassword());

        tvAdmin.setText((user.isAdmin ? "Yes" : "No"));
    }

    // private void deleteUser() {
       // if (currentUser == null) {
          //  Toast.makeText(this, "No user to delete", Toast.LENGTH_SHORT).show();
          //  return;
      //  }

      //  progressBar.setVisibility(View.VISIBLE);

      //  databaseService.deleteUserById(currentUser.getId(), new DatabaseService.DatabaseCallback<Void>() {
        //    @Override
        //    public void onCompleted(Void unused) {
          //      progressBar.setVisibility(View.GONE);
           //     Toast.makeText(UserProfileActivity.this, "User deleted", Toast.LENGTH_SHORT).show();
          //      finish(); // go back to users list
          //  }

         //   @Override
         //   public void onFailed(Exception e) {
         //       progressBar.setVisibility(View.GONE);
          //      Log.e(TAG, "Delete failed", e);
         //       Toast.makeText(UserProfileActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
          //  }
      //  });
   // }

    }
