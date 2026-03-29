package com.example.drowsydrivingdetection.ui.pages;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.media.MediaPlayer;

import android.net.Uri;
import android.content.pm.PackageManager;
import android.os.Environment;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.example.drowsydrivingdetection.R;
import com.example.drowsydrivingdetection.ui.nav.NavActivity;
import com.example.drowsydrivingdetection.viewmodel.ProfileViewModel;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

public class ProfileActivity extends NavActivity {

    private TextView userName;
    private TextView userEmail;
    private Button btnUpload;
    private Button btnLogout;
    private SwitchMaterial switchNarcolepsy;
    private SwitchMaterial switchSleepApnea;
    private SwitchMaterial switchInsomnia;
    private SwitchMaterial switchAuditoryAlerts;
    private SwitchMaterial switchVisualAlerts;
    private TextView auditoryAlertsCount;
    private TextView visualAlertsCount;
    private Uri cameraImageUri;
    private ShapeableImageView profilePicture;
    private TextView passBanner;
    private TextView errorBanner;
    private String originalName;
    private String originalEmail;
    private ImageView btnEdit;
    private ImageView saveProfile;
    private ImageView cancelEdit;
    private TextInputEditText editName;
    private TextInputEditText editEmail;

    private RadioGroup soundChoices;
    private Button btnPreview;
    private MediaPlayer previewPlayer;
    private int selectedSound;

    private ProfileViewModel viewModel;

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

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        initializeViews();
        setupBottomNavigation();
        loadUserData();
        setupListeners();

        //Ahmed: get a sound ready to play
        loadSound();
        //Ahmed: pick which sound to use
        selectSound();
    }

    private void initializeViews() {
        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        btnUpload = findViewById(R.id.btnUpload);
        btnEdit = findViewById(R.id.editProfile);
        cancelEdit = findViewById(R.id.cancelEdit);
        saveProfile = findViewById(R.id.saveProfile);

        editName = findViewById(R.id.etEditName);
        editEmail = findViewById(R.id.etEditEmail);

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

        //Ahmed: list of sounds
        soundChoices = findViewById(R.id.soundChoices);
        //Ahmed: play sound
        btnPreview = findViewById(R.id.btnPreview);
    }

    private void loadUserData() {
        // Load user data from SharedPreferences
        ProfileViewModel.ProfileData data = viewModel.loadProfileData();

        userName.setText(data.fullName);
        userEmail.setText(data.email);
        originalName = data.fullName;
        originalEmail = data.email;

        // Load switch states
        switchNarcolepsy.setChecked(data.narcolepsy);
        switchSleepApnea.setChecked(data.sleepApnea);
        switchInsomnia.setChecked(data.insomnia);
        switchAuditoryAlerts.setChecked(data.auditoryAlerts);
        switchVisualAlerts.setChecked(data.visualAlerts);

        // Load alert counts
        auditoryAlertsCount.setText(String.valueOf(data.audioAlertCount));
        visualAlertsCount.setText(String.valueOf(data.visualAlertCount));

        // Profile Picture - Kaavya
        if (data.profilePhotoUri != null) {
            profilePicture.setImageURI(Uri.parse(data.profilePhotoUri));
        }
    }

    //Ahmed: choose a sound and save it and play it when you press preview button
    private void selectSound() {
        soundChoices.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                selectedSound = sounds(checkedId); //Ahmed: pick a sound
                saveSound(selectedSound); //Ahmed: save sound
            }
        });

        btnPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previewSound(); //Ahmed: play sound
            }
        });
    }

    //Ahmed: connect button to sound
    private int sounds(int id) {
        if (id == R.id.sound1) return R.raw.sound1;
        if (id == R.id.sound2) return R.raw.sound2;
        if (id == R.id.sound3) return R.raw.sound3;
        if (id == R.id.sound4) return R.raw.sound4;
        if (id == R.id.sound5) return R.raw.sound5;
        if (id == R.id.sound6) return R.raw.sound6;
        if (id == R.id.sound7) return R.raw.sound7;
        if (id == R.id.sound8) return R.raw.sound8;
        if (id == R.id.sound9) return R.raw.chime_final;
        if (id == R.id.sound10) return R.raw.audio_alert;
        return R.raw.chime_final; //Ahmed: default sound
    }

    //Ahmed: play sound one time
    private void previewSound() {
        if (previewPlayer != null) previewPlayer.release(); // stop sound
        previewPlayer = MediaPlayer.create(this, selectedSound);
        previewPlayer.start();
    }

    //Ahmed: save sound
    private void saveSound(int resId) {
        getSharedPreferences("DrowsyDriverPrefs", MODE_PRIVATE)
                .edit()
                .putInt("selected_sound", resId)
                .apply();
    }

    //Ahmed: load saved sound
    private void loadSound() {
        int saved = getSharedPreferences("DrowsyDriverPrefs", MODE_PRIVATE).getInt("selected_sound", R.raw.chime_final);
        selectedSound = saved;

        if (saved == R.raw.sound1) soundChoices.check(R.id.sound1);
        else if (saved == R.raw.sound2) soundChoices.check(R.id.sound2);
        else if (saved == R.raw.sound3) soundChoices.check(R.id.sound3);
        else if (saved == R.raw.sound4) soundChoices.check(R.id.sound4);
        else if (saved == R.raw.sound5) soundChoices.check(R.id.sound5);
        else if (saved == R.raw.sound6) soundChoices.check(R.id.sound6);
        else if (saved == R.raw.sound7) soundChoices.check(R.id.sound7);
        else if (saved == R.raw.sound8) soundChoices.check(R.id.sound8);
        else if (saved == R.raw.audio_alert) soundChoices.check(R.id.sound10);
        else soundChoices.check(R.id.sound9); //Ahmed: default sound
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

        viewModel.saveProfilePhoto(imageUri.toString());
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
            viewModel.saveSwitchState("narcolepsy", isChecked);
        });

        switchSleepApnea.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.saveSwitchState("sleepApnea", isChecked);
        });

        switchInsomnia.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.saveSwitchState("insomnia", isChecked);
        });

        switchAuditoryAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.saveSwitchState("auditoryAlerts", isChecked);
        });

        switchVisualAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.saveSwitchState("visualAlerts", isChecked);
        });

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enterEditMode();
            }
        });

        cancelEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeEditMode();
            }
        });

        saveProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfileChanges();
            }
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
        viewModel.logout();
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

    private void enterEditMode() {
        //hide display mode views
        userName.setVisibility(View.GONE);
        userEmail.setVisibility(View.GONE);
        btnEdit.setVisibility(View.GONE);

        editEmail.setVisibility(View.VISIBLE);
        editName.setVisibility(View.VISIBLE);
        findViewById(R.id.tilEditName).setVisibility(View.VISIBLE);
        //findViewById(R.id.tilEditEmail).setVisibility(View.VISIBLE);
        saveProfile.setVisibility(View.VISIBLE);
        cancelEdit.setVisibility(View.VISIBLE);

        editName.setText(originalName);
        editEmail.setText(originalEmail);
        editName.requestFocus();
    }

    private void closeEditMode() {
        //show display mode views
        userName.setVisibility(View.VISIBLE);
        userEmail.setVisibility(View.VISIBLE);
        btnEdit.setVisibility(View.VISIBLE);

        findViewById(R.id.tilEditName).setVisibility(View.GONE);
        //findViewById(R.id.tilEditEmail).setVisibility(View.GONE);

        saveProfile.setVisibility(View.GONE);
        cancelEdit.setVisibility(View.GONE);
    }

    private void saveProfileChanges() {
        String newName = editName.getText().toString().trim();

        ProfileViewModel.SaveProfileResult result = viewModel.saveProfileChanges(newName, originalEmail);

        if (result.isSuccess) {
            originalName = result.updatedName;
            userName.setText(result.updatedName);
            closeEditMode();
            showPass(result.message);
        } else {
            showError(result.message);
        }
    }

}
