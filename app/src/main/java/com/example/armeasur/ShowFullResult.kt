package com.example.armeasur

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.example.armeasur.databinding.ActivityShowFullResultBinding

class ShowFullResult : AppCompatActivity() {
    lateinit var binding:ActivityShowFullResultBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityShowFullResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val urll=intent.getStringExtra("url")
        val namee=intent.getStringExtra("name")
        val timee=intent.getStringExtra("date")
        Glide.with(this)
            .load(urll.toString())
            .into(binding.fullimage)
        binding.date.setText(timee.toString())
        binding.name.setText(namee.toString())
    }
}