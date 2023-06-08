package com.example.armeasur;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.armeasur.adapter.MyAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class Measure extends Fragment {

    private FloatingActionButton btnCam;
    private FloatingActionButton btnlogout;
    private ImageView imgview;
    private static final int REQUEST_CODE = 22;

    // Firebase Storage
    private FirebaseStorage storage;
    private FirebaseAuth auth;
    private DatabaseReference dateRef;
    private DatabaseReference timeRef;
    private RecyclerView recyclerView;
    private DatabaseReference dref;
    private ArrayList<UserData> arrayList;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_measure, container, false);
        btnCam = view.findViewById(R.id.btnCam);
        btnlogout = view.findViewById(R.id.logout);
        imgview = view.findViewById(R.id.imgview);

        TextView textView;
        textView = view.findViewById(R.id.wish);
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);


        BottomNavigationView bottomNavigationView = view.findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                if (item.getItemId() == R.id.menu_home) {

                    View bottomSheetView = getLayoutInflater().inflate(R.layout.bottombar_view, null);

                    // Create a BottomSheetDialog with the inflated layout
                    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
                    bottomSheetDialog.setContentView(bottomSheetView);
                    recyclerView = bottomSheetView.findViewById(R.id.recyclerview);
                    LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
                    recyclerView.setLayoutManager(layoutManager);
                    recyclerView.setHasFixedSize(true);
                    arrayList = new ArrayList<UserData>();
                    getdata();
                    bottomSheetDialog.show();

                    return true;
                }
                if (item.getItemId() == R.id.menu_logout) {

                    FirebaseAuth.getInstance().signOut();
                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    Login fragment = new Login();
                    fragmentTransaction.replace(R.id.fragment_container, fragment);
                    fragmentTransaction.commit();
                    Toast.makeText(getContext(), "Successfully Logout", Toast.LENGTH_SHORT).show();

                    return true;
                }
                return false;
            }
        });


        String greeting;

        if (hour >= 0 && hour < 12) {
            textView.setText("Good\nMorning 🌞");
//            textView.setBackgroundDrawable(ContextCompat.getDrawable(getContext(),R.drawable.good));
//            textView.setTextColor(this.getResources().getColor(R.color.black));
        } else if (hour >= 12 && hour < 18) {
            textView.setText("Good\nAfternoon ☀️");
//            textView.setBackgroundDrawable(ContextCompat.getDrawable(getContext(),R.drawable.afternoon));
//            textView.setTextColor(this.getResources().getColor(androidx.cardview.R.color.cardview_light_background));
        } else {
            textView.setText("Good\nEvening 🌛");
//            textView.setBackgroundDrawable(ContextCompat.getDrawable(getContext(),R.drawable.enening));
//            textView.setTextColor(this.getResources().getColor(R.color.white));

        }

        // Initialize Firebase Storage
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();

        // Create a Firebase Realtime Database reference for date and time separately
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        dateRef = database.getReference().child("dates");
        timeRef = database.getReference().child("times");
        btnlogout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                Login fragment = new Login();
                fragmentTransaction.replace(R.id.fragment_container, fragment);
                fragmentTransaction.commit();
                Toast.makeText(getContext(), "Successfully Logout", Toast.LENGTH_SHORT).show();

            }
        });
        btnCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                startActivityForResult(cameraIntent, REQUEST_CODE);

                Intent myIntent = new Intent(Measure.this.getActivity(), MainActivity2.class);
                startActivity(myIntent);
            }
        });

        return view;
    }

    private void getdata() {
        DatabaseReference dbref = FirebaseDatabase.getInstance().getReference(auth.getCurrentUser().getUid());

        dbref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    ArrayList<UserData> userArrayList = new ArrayList<>();

                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        UserData user = userSnapshot.getValue(UserData.class);
                        userArrayList.add(user);
                    }

                    recyclerView.setAdapter(new MyAdapter(userArrayList));
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle onCancelled event
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            imgview.setImageBitmap(photo);

            // Convert Bitmap to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            photo.compress(CompressFormat.JPEG, 100, baos);
            byte[] imageData = baos.toByteArray();

            // Generate a unique image name
            String imageName = UUID.randomUUID().toString() + ".jpg";

            // Check if the user is authenticated
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser != null) {
                // User is logged in
                String userId = currentUser.getUid();

                // Create a reference to the storage path where you want to store the image
                StorageReference storageRef = storage.getReference().child(userId).child(imageName);

                // Upload the byte array to Firebase Storage
                UploadTask uploadTask = storageRef.putBytes(imageData);
                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Image upload successful
                        // You can retrieve the download URL of the image using taskSnapshot.getDownloadUrl() and save it to the Firebase Database if needed.
                        // Example: String downloadUrl = taskSnapshot.getDownloadUrl().toString();

                        // Get the current date and time
                        Date currentDate = new Date();
                        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentDate);
                        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(currentDate);

                        // Store the date and time separately in the Firebase Realtime Database
                        DatabaseReference userDateRef = dateRef.child(userId).push();
                        DatabaseReference userTimeRef = timeRef.child(userId).push();
                        userDateRef.setValue(date);
                        userTimeRef.setValue(time);

                        // Perform further actions with the uploaded image, date, and time
//                        getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, new ImgRetrive())
//                                .addToBackStack(null).commit();
                        Toast.makeText(getContext(), "Image uploaded successfully. Date: " + date + ", Time: " + time, Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Image upload failed
//                        getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, new ImgRetrive())
//                                        .addToBackStack(null).commit();
                        Toast.makeText(getContext(), "Image upload failed", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // User is not logged in
                Toast.makeText(getContext(), "User is not logged in", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "CANCELLED", Toast.LENGTH_SHORT).show();
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
