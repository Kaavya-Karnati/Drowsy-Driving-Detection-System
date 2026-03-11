package com.example.drowsydrivingdetection;


import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import android.net.Uri;
import android.content.pm.PackageManager;
import android.os.Environment;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


import com.google.android.material.switchmaterial.SwitchMaterial;

public class ProfileActivity extends NavActivity {

    private TextView userName;
    private TextView userEmail;
    private Button btnUpload;
    private Button btnChange;
    private Button btnLogout;
    private SwitchMaterial switchNarcolepsy;
    private SwitchMaterial switchSleepApnea;
    private SwitchMaterial switchInsomnia;
    private SwitchMaterial switchAuditoryAlerts;
    private SwitchMaterial switchVisualAlerts;
    private TextView auditoryAlertsCount;
    private TextView visualAlertsCount;
    private SharedPreferences sharedPreferences;
    private Uri cameraImageUri;
    private ShapeableImageView profilePicture;
    private TextView passBanner;
    private TextView errorBanner;

    // Opens the photo gallery and receives the selected image
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri imageUri = result.getData().getData();
            if (imageUri != null) {
                handleImageSelected(imageUri);
            }
        }
    });

    // Lanches the camera
    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
        if (success && cameraImageUri != null) {
            handleImageSelected(cameraImageUri);
        }
    });

    // Requests the user for camera permissions
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            openCamera();
                        } else {
                            //Toast.makeText(this, "Camera permission is required to take a photo.", Toast.LENGTH_SHORT).show();
                            showError("Camera permission is required to take a photo.");
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logged_in_profile_page);

        sharedPreferences = getSharedPreferences("DrowsyDriverPrefs", MODE_PRIVATE);

        initializeViews();
        setupBottomNavigation();
        loadUserData();
        setupListeners();
    }

    private void initializeViews() {
        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        btnUpload = findViewById(R.id.btnUpload);
        //btnChange = findViewById(R.id.btnChange);
        btnLogout = findViewById(R.id.btnLogout);
        switchNarcolepsy = findViewById(R.id.switchNarcolepsy);
        switchSleepApnea = findViewById(R.id.switchSleepApnea);
        switchInsomnia = findViewById(R.id.switchInsomnia);
        switchAuditoryAlerts = findViewById(R.id.switchAuditoryAlerts);
        switchVisualAlerts = findViewById(R.id.switchVisualAlerts);
        auditoryAlertsCount = findViewById(R.id.auditoryAlertsCount);
        visualAlertsCount = findViewById(R.id.visualAlertsCount);

        profilePicture = findViewById(R.id.profilePicture);
        passBanner = findViewById(R.id.passBanner);
        errorBanner = findViewById(R.id.errorBanner);
    }

    private void loadUserData() {
        // Load user data from SharedPreferences
        userEmail.setText(sharedPreferences.getString("userEmail", "default@gmail.com"));
        String email = (String) userEmail.getText();
        String firstName = sharedPreferences.getString("userFirstName_" + email, "");
        String lastName = sharedPreferences.getString("userLastName_" + email, "");
        String fullName = sharedPreferences.getString("userName_" + email, firstName + " " + lastName);
        userName.setText(fullName);

        // Load switch states
        switchNarcolepsy.setChecked(sharedPreferences.getBoolean("narcolepsy", false));
        switchSleepApnea.setChecked(sharedPreferences.getBoolean("sleepApnea", false));
        switchInsomnia.setChecked(sharedPreferences.getBoolean("insomnia", false));
        switchAuditoryAlerts.setChecked(sharedPreferences.getBoolean("auditoryAlerts", true));
        switchVisualAlerts.setChecked(sharedPreferences.getBoolean("visualAlerts", true));

        // Load alert counts
        int audioCount = sharedPreferences.getInt("audio_alert", 0);
        int visualCount = sharedPreferences.getInt("visual_alert", 0);
        auditoryAlertsCount.setText(String.valueOf(audioCount));
        visualAlertsCount.setText(String.valueOf(visualCount));

        // Profile Picture - Kaavya
        String profilePhoto = sharedPreferences.getString(getUserPhoto(), null);
        if(profilePhoto != null){
            profilePicture.setImageURI(Uri.parse(profilePhoto));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
    }

    // User to choose between gallery selection or take a photo
    private void showImagePickerDialog() {

        final String[] options = {"Choose from Gallery", "Take Photo"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Profile Picture");

        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (which == 0) {
                    openGallery();
                } else {
                    checkCameraPermission();
                }
            }
        });

        builder.show();
    }

    // Selects the image and updates the profile pic
    private void handleImageSelected(Uri imageUri) {

        try {
            getContentResolver().takePersistableUriPermission(
                    imageUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}

        sharedPreferences.edit()
                .putString(getUserPhoto(), imageUri.toString())
                .apply();

        profilePicture.setImageURI(imageUri);

        //Toast.makeText(this, "Profile photo updated!", Toast.LENGTH_SHORT).show();
        showPass("Profile photo updated!");
    }

    // Camera Permissions
    private void checkCameraPermission() {

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        }
    }

    // Opens the gallery to choose the image
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        galleryLauncher.launch(intent);
    }

    // Opens the camera
    private void openCamera() {

        try {
            File photoFile = createImageFile();

            cameraImageUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    photoFile);

            cameraLauncher.launch(cameraImageUri);

        } catch (IOException e) {
            // Toast.makeText(this, "Could not open camera", Toast.LENGTH_SHORT).show();
            showError("Could not open camera");
        }
    }

    // Creates a temp image file
    private File createImageFile() throws IOException {

        String timeStamp = new SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        return File.createTempFile(
                "PROFILE_" + timeStamp,
                ".jpg",
                storageDir);
    }
    private void setupListeners() {
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(ProfileActivity.this, "Upload profile picture", Toast.LENGTH_SHORT).show();
                showImagePickerDialog();
            }
        });

        /*
        btnChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(ProfileActivity.this, "Change profile picture", Toast.LENGTH_SHORT).show();
                showImagePickerDialog();
            }
        });
        */

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutDialog();
            }
        });


        // Save switch states when changed
        switchNarcolepsy.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("narcolepsy", isChecked).apply();
        });

        switchSleepApnea.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("sleepApnea", isChecked).apply();
        });

        switchInsomnia.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("insomnia", isChecked).apply();
        });

        switchAuditoryAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("auditoryAlerts", isChecked).apply();
        });

        switchVisualAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("visualAlerts", isChecked).apply();
        });
    }

    // Ahmed's Code
    private void showError(String message) {
        errorBanner.setText(message);
        errorBanner.setVisibility(View.VISIBLE);
        errorBanner.setAlpha(0f);
        errorBanner.animate()
                .alpha(1f)
                .setDuration(300)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        errorBanner.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                errorBanner.animate()
                                        .alpha(0f)
                                        .setDuration(300)
                                        .withEndAction(new Runnable() {
                                            @Override
                                            public void run() {
                                                errorBanner.setVisibility(View.GONE);
                                            }
                                        });
                            }
                        }, 2000);
                    }
                });
    }


    private void showPass(String message) {
        passBanner.setText(message);
        passBanner.setVisibility(View.VISIBLE);
        passBanner.setAlpha(1f);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                passBanner.animate()
                        .alpha(0f)
                        .setDuration(500)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                passBanner.setVisibility(View.GONE);
                            }
                        });
            }
        }, 5000);
    }
    // End of Ahmed's Code

    private void handleLogout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Only clear login session, not account credentials
        editor.remove("isLoggedIn");
        editor.remove("isGuest");
        editor.apply();

        //Toast.makeText(ProfileActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        showPass("Logged out successfully");

        // Navigate to sign-in page
        Intent intent = new Intent(ProfileActivity.this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handleLogout();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    public void onAlertRecordsClicked(View view) {
            Intent intent = new Intent(this, AlertsDashboard.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
    }

    private String getUserPhoto() {
        String email = sharedPreferences.getString("userEmail", "guest");
        return "profilePhotoUri_" + email;
    }

}