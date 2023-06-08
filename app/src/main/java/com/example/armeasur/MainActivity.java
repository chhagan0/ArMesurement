package com.example.armeasur;

import static android.app.PendingIntent.getActivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private FirebaseAuth auth;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Check if user is already logged in
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        if (isLoggedIn) {
            // User is already logged in, navigate to the desired activity or fragment
            navigateToMeasureFragment();
        } else {
            // User is not logged in, show the create account fragment
            navigateToCreateAccountFragment();
        }
    }

    public void navigateToCreateAccountFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new Creat_Account())
                .commit();
    }

    public void navigateToLoginFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new Login())
                .commit();
    }

    public void navigateToMeasureFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new Measure())
                .commit();
    }

    public void handleSuccessfulLogin() {
        // Update the login state in SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.apply();

        // Navigate to the Measure fragment
        navigateToMeasureFragment();

        Toast.makeText(this, "Login Success", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            Login fragment = new Login();
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            fragmentTransaction.commit();
        }
    }
}
