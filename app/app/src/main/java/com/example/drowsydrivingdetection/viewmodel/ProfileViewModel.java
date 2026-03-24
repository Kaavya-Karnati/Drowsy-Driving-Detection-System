package com.example.drowsydrivingdetection.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class ProfileViewModel extends AndroidViewModel {

    private final SharedPreferences sharedPreferences;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences("DrowsyDriverPrefs", Application.MODE_PRIVATE);
    }

    public ProfileData loadProfileData() {
        String email = sharedPreferences.getString("userEmail", "default@gmail.com");
        String firstName = sharedPreferences.getString("userFirstName_" + email, "");
        String lastName = sharedPreferences.getString("userLastName_" + email, "");
        String fullName = sharedPreferences.getString("userName_" + email, firstName + " " + lastName);

        boolean narcolepsy = sharedPreferences.getBoolean("narcolepsy", false);
        boolean sleepApnea = sharedPreferences.getBoolean("sleepApnea", false);
        boolean insomnia = sharedPreferences.getBoolean("insomnia", false);
        boolean auditoryAlerts = sharedPreferences.getBoolean("auditoryAlerts", true);
        boolean visualAlerts = sharedPreferences.getBoolean("visualAlerts", true);

        int audioAlertCount = sharedPreferences.getInt("audio_alert", 0);
        int visualAlertCount = sharedPreferences.getInt("visual_alert", 0);

        String profilePhotoUri = sharedPreferences.getString(getProfilePhotoKey(), null);

        return new ProfileData(
                fullName, email,
                narcolepsy, sleepApnea, insomnia,
                auditoryAlerts, visualAlerts,
                audioAlertCount, visualAlertCount,
                profilePhotoUri
        );
    }

    public void saveSwitchState(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    public void saveProfilePhoto(String uriString) {
        sharedPreferences.edit()
                .putString(getProfilePhotoKey(), uriString)
                .apply();
    }

    public SaveProfileResult saveProfileChanges(String newName, String currentEmail) {
        //validate name
        if (newName == null || newName.trim().isEmpty()) {
            return SaveProfileResult.error("Name cannot be empty");
        }

        //split name into first and last name
        String trimmedName = newName.trim();
        String[] nameParts = trimmedName.split(" ", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        //save to SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userFirstName_" + currentEmail, firstName);
        editor.putString("userLastName_" + currentEmail, lastName);
        editor.putString("userName_" + currentEmail, trimmedName);
        editor.apply();

        return SaveProfileResult.success(trimmedName, "Profile updated successfully");
    }

    public void logout() {
        // Only clear login session, not account credentials
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("isLoggedIn");
        editor.remove("isGuest");
        editor.apply();
    }

    private String getProfilePhotoKey() {
        String email = sharedPreferences.getString("userEmail", "guest");
        return "profilePhotoUri_" + email;
    }

    public static class ProfileData {
        public final String fullName;
        public final String email;
        public final boolean narcolepsy;
        public final boolean sleepApnea;
        public final boolean insomnia;
        public final boolean auditoryAlerts;
        public final boolean visualAlerts;
        public final int audioAlertCount;
        public final int visualAlertCount;
        public final String profilePhotoUri;

        public ProfileData(String fullName, String email,
                           boolean narcolepsy, boolean sleepApnea, boolean insomnia,
                           boolean auditoryAlerts, boolean visualAlerts,
                           int audioAlertCount, int visualAlertCount,
                           String profilePhotoUri) {
            this.fullName = fullName;
            this.email = email;
            this.narcolepsy = narcolepsy;
            this.sleepApnea = sleepApnea;
            this.insomnia = insomnia;
            this.auditoryAlerts = auditoryAlerts;
            this.visualAlerts = visualAlerts;
            this.audioAlertCount = audioAlertCount;
            this.visualAlertCount = visualAlertCount;
            this.profilePhotoUri = profilePhotoUri;
        }
    }

    public static class SaveProfileResult {
        public final boolean isSuccess;
        public final String updatedName;
        public final String message;

        private SaveProfileResult(boolean isSuccess, String updatedName, String message) {
            this.isSuccess = isSuccess;
            this.updatedName = updatedName;
            this.message = message;
        }

        public static SaveProfileResult success(String updatedName, String message) {
            return new SaveProfileResult(true, updatedName, message);
        }

        public static SaveProfileResult error(String message) {
            return new SaveProfileResult(false, null, message);
        }
    }
}
