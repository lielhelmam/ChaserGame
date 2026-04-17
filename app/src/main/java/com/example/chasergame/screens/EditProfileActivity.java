package com.example.chasergame.screens;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chasergame.R;
import com.example.chasergame.models.User;
import com.example.chasergame.services.DatabaseService;
import com.example.chasergame.utils.ImageUtils;
import com.example.chasergame.utils.Validator;

import java.io.InputStream;

public class EditProfileActivity extends BaseActivity {
    private EditText etUsername, etEmail, etPassword;
    private ImageView ivProfilePic;
    private User user;
    private String encodedImage = null;

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    ivProfilePic.setImageBitmap(imageBitmap);
                    encodedImage = ImageUtils.encodeImage(imageBitmap);
                }
            }
    );
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
            }
    );
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        ivProfilePic.setImageBitmap(bitmap);
                        encodedImage = ImageUtils.encodeImage(bitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.EditProfilePage), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initUI();
    }

    private void initUI() {
        etUsername = findViewById(R.id.et_edit_username);
        etEmail = findViewById(R.id.et_edit_email);
        etPassword = findViewById(R.id.et_edit_password);
        ivProfilePic = findViewById(R.id.iv_edit_profile_pic);
        ImageButton btnChangePic = findViewById(R.id.btn_change_pic);
        Button btnSave = findViewById(R.id.btn_save_profile);
        Button btnCancel = findViewById(R.id.btn_cancel_profile);

        user = authService.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        etUsername.setText(user.getUsername());
        etEmail.setText(user.getEmail());
        etPassword.setText(user.getPassword());

        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            ivProfilePic.setImageBitmap(ImageUtils.decodeImage(user.getProfileImage()));
            encodedImage = user.getProfileImage();
        }

        ivProfilePic.setOnClickListener(v -> showImagePickerDialog());
        btnChangePic.setOnClickListener(v -> showImagePickerDialog());
        btnCancel.setOnClickListener(v -> navigateTo(MainActivity.class, true));
        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void showImagePickerDialog() {
        String[] options = {"Camera", "Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Select Profile Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndOpen();
                    } else {
                        openGallery();
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(takePictureIntent);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void validateAndSave() {
        String name = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (!Validator.isNameValid(name)) {
            etUsername.setError("Invalid username");
            return;
        }
        if (!Validator.isEmailValid(email)) {
            etEmail.setError("Invalid email");
            return;
        }
        if (!Validator.isPasswordValid(pass)) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        boolean emailChanged = user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email);
        if (emailChanged) {
            databaseService.checkIfEmailExists(email, new DatabaseService.DatabaseCallback<Boolean>() {
                @Override
                public void onCompleted(Boolean exists) {
                    if (exists) etEmail.setError("Email exists");
                    else performUpdate(name, email, pass);
                }

                @Override
                public void onFailed(Exception e) {
                }
            });
        } else {
            performUpdate(name, email, pass);
        }
    }

    private void performUpdate(String name, String email, String pass) {
        user.setProfileImage(encodedImage);
        authService.updateProfile(user, name, email, pass, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void unused) {
                Toast.makeText(EditProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();
                navigateTo(MainActivity.class, true);
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(EditProfileActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
