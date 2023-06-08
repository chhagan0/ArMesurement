package com.example.armeasur.adapter

import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.util.Log.d
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.armeasur.MainActivity2
import com.example.armeasur.R
import com.example.armeasur.ShowFullResult
import com.example.armeasur.UserData
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class MyAdapter(private val userList: ArrayList<UserData>) :
    RecyclerView.Adapter<MyAdapter.MyViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_view, parent, false)
        return MyViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentitem = userList[position]
        val context = holder.itemView.context

        holder.resultdate.text = currentitem.time
        holder.resultname.text = currentitem.name
        Glide.with(holder.itemView)
            .load(currentitem.url)
            .into(holder.resultimage)
holder.itemView.setOnClickListener {
    val intent = Intent(context, ShowFullResult::class.java)
    intent.putExtra("url", currentitem.url.toString())
    intent.putExtra("name", currentitem.name.toString())
    intent.putExtra("date", currentitem.time)
    context.startActivity(intent)

}
        holder.btndelete.setOnClickListener {
            deleteData(context, currentitem.name)
        }
    }

    private fun deleteData(context: Context, name: String?) {
        val auth = FirebaseAuth.getInstance()
        d("CHAGAN", "userid ${auth.currentUser!!.uid}")
        var dialog = Dialog(context)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(R.layout.button_delete)

        val positiveButton = dialog.findViewById<MaterialCardView>(R.id.btndelete)
        val negativeButton = dialog.findViewById<MaterialCardView>(R.id.btncancel)
        negativeButton.setOnClickListener { dialog.dismiss() }
        positiveButton.setOnClickListener {

            dialog.dismiss()


            val progres = ProgressDialog(context)
            progres.show()
            progres.setCancelable(false)
            progres.setMessage("Please wait...")

            val databaseRef = FirebaseDatabase.getInstance().getReference(auth.currentUser!!.uid)
                .child(name.toString())
            databaseRef.removeValue()
                .addOnSuccessListener {
                    d("CHAGAN", "realtime data remove")
                    val storageRef =
                        FirebaseStorage.getInstance().getReference("${auth.currentUser!!.uid}/")
                            .child(name.toString())
                    storageRef.delete()
                        .addOnSuccessListener {
                            progres.dismiss()
                            Toast.makeText(context, "Successfuly deleted!!", Toast.LENGTH_SHORT)
                                .show()
                        }
                        .addOnFailureListener {
                            d("CHAGAN", it.message.toString())
                        }

                }
                .addOnFailureListener {
                    // Failed to delete data
                }


        }
        dialog.show()


    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val resultimage: ImageView = itemView.findViewById(R.id.resultimage)
        val resultname: TextView = itemView.findViewById(R.id.name)
        val resultdate: TextView = itemView.findViewById(R.id.date)
        val btndelete: ImageView = itemView.findViewById(R.id.delete)

    }

    fun DeleteImages(id: String, context: Context, progres: ProgressDialog) {


//        val auth=FirebaseAuth.getInstance()
//        val storageRef = FirebaseStorage.getInstance().getReference()
//        val folderRef = storageRef.child(auth.currentUser!!.uid+id)
//
//
//        folderRef.listAll()
//            .addOnSuccessListener { listResult ->
//                listResult.items.forEach { item ->
//                    item.delete()
//                        .addOnSuccessListener {
//
//                                (context as Activity).finish()
//                                Toast.makeText(context, "Data Deleted", Toast.LENGTH_SHORT).show()
//                                progres.dismiss()
//                                val intent = Intent(context, MainActivity2::class.java)
//                                context.startActivity(intent)
//
//
//
////                            Log.d(TAG, "Image ${item.path} successfully deleted!")
//                        }
//                        .addOnFailureListener { e ->
////                            Log.w(TAG, "Error deleting image ${item.path}", e)
//                        }
//                }
//
//                folderRef.delete()
//                    .addOnSuccessListener {
//                     }
//                    .addOnFailureListener { e ->
//                     }
//            }
//            .addOnFailureListener { e ->
//             }
//
//
//
//    }

    }
}