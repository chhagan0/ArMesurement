package com.example.armeasur

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.armeasur.databinding.ActivityMain2Binding
import com.example.armeasur.databinding.ActivityMainBinding
import com.example.armeasur.viewmodel.ViewModel


class MainActivity2 : AppCompatActivity() {
    val viewModel : ViewModel by viewModels()
    private lateinit var binding : ActivityMain2Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            val toast = Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
            toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
            if (!shouldShowRequestPermissionRationale(this.toString())) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this)
            }
            finish()
        }
    }


}