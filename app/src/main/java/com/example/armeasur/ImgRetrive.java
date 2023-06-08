package com.example.armeasur;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class ImgRetrive extends Fragment {
    private ImageView retrievedImageView;
    private TextView retrievedDateTextView;
    private TextView retrievedTimeTextView;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference dateRef;
    private DatabaseReference timeRef;
    private StorageReference storageRef;
    private FirebaseDatabase database;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_img_retrive, container, false);

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();
        dateRef = database.getReference().child("dates");
        timeRef = database.getReference().child("times");
        storageRef = FirebaseStorage.getInstance().getReference();

        // Initialize views
        retrievedImageView = view.findViewById(R.id.retrievedImageView);
        retrievedDateTextView = view.findViewById(R.id.retrievedDateTextView);
        retrievedTimeTextView = view.findViewById(R.id.retrievedTimeTextView);

        // Retrieve the image and date/time for the current user
        retrieveImageAndDateTime();
        return view;
    }

    private void retrieveImageAndDateTime() {
        // Check if the user is authenticated
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Retrieve the latest date for the user from the Realtime Database
            dateRef.child(userId).limitToLast(1).get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                @Override
                public void onSuccess(DataSnapshot dataSnapshot) {
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        String date = childSnapshot.getValue(String.class);
                        retrievedDateTextView.setText(date);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getContext(), "Failed to retrieve date", Toast.LENGTH_SHORT).show();
                }
            });

            // Retrieve the latest time for the user from the Realtime Database
            timeRef.child(userId).limitToLast(1).get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                @Override
                public void onSuccess(DataSnapshot dataSnapshot) {
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        String time = childSnapshot.getValue(String.class);
                        retrievedTimeTextView.setText(time);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getContext(), "Failed to retrieve time", Toast.LENGTH_SHORT).show();
                }
            });

            // Retrieve the latest image path for the user from the Realtime Database
            DatabaseReference imagePathRef = database.getReference().child("users").child(userId).child("imagePath");
            imagePathRef.get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                @Override
                public void onSuccess(DataSnapshot dataSnapshot) {
                    String imagePath = dataSnapshot.getValue(String.class);

                    // Convert the image path to a StorageReference
                    StorageReference imageStorageRef = storageRef.child(imagePath);

                    // Retrieve the image from Firebase Storage and display it in the ImageView
                    imageStorageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Picasso.get().load(uri).into(retrievedImageView);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getContext(), "Failed to retrieve image", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getContext(), "Failed to retrieve image path", Toast.LENGTH_SHORT).show();
                }
            });

        }
    }
}
