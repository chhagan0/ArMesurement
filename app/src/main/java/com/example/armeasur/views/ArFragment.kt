package com.example.armeasur.views


import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color.rgb
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.util.Log.d
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.armeasur.MainActivity
import com.example.armeasur.R
import com.example.armeasur.databinding.FragmentArBinding
import com.example.armeasur.viewmodel.ViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3.zero
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.IntBuffer
import java.text.SimpleDateFormat
import java.util.*


open class ArFragment : Fragment(com.example.armeasur.R.layout.fragment_ar) {
    private lateinit var viewRenderable: ViewRenderable
    private lateinit var distanceInMeters: CardView
    private lateinit var lineBetween: AnchorNode
    private var lengthLabel: AnchorNode? = null
    private var node2Pos: Vector3? = null
    private var node1Pos: Vector3? = null
    private var initialAnchor: AnchorNode? = null
    lateinit var binding: FragmentArBinding
    lateinit var arFragment: ArFragment
    private var anchorNodeTemp: AnchorNode? = null
    private var pointRender: ModelRenderable? = null
    private var aimRender: ModelRenderable? = null
    private var widthLineRender: ModelRenderable? = null
    private val REQUEST_CODE = 22
    private var heightLineRender: ModelRenderable? = null
    private val currentAnchorNode = ArrayList<AnchorNode>()
    private val labelArray: ArrayList<AnchorNode> = ArrayList()
    private val currentAnchor = ArrayList<Anchor?>()
    private var totalLength = 0f
    private var difference: Vector3? = null
    private val tempAnchorNodes: ArrayList<AnchorNode> = arrayListOf()
    private var storage: FirebaseStorage? = null

    private var auth: FirebaseAuth? = null
    private val dateRef: DatabaseReference? = null
    private val timeRef: DatabaseReference? = null
    private var storageref = Firebase.storage
    private val viewModel: ViewModel by activityViewModels()

    @SuppressLint("ResourceType")
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentArBinding.bind(view)

        storageref = FirebaseStorage.getInstance()

        auth = FirebaseAuth.getInstance()
        arFragment =
            childFragmentManager.findFragmentById(com.example.armeasur.R.id.ux_fragment) as ArFragment

        binding.btnClear.setOnClickListener {
            clearAnchors()
        }

        binding.floatingActionButton2.setOnClickListener {


            binding.btnClear.visibility = View.GONE
            binding.btnAdd.visibility = View.GONE
            binding.floatingActionButton2.visibility = View.GONE
            val inflater = LayoutInflater.from(view.context)
            val dialogView = inflater.inflate(R.layout.screenshotview, null)

            val cancelbtn: MaterialButton = dialogView.findViewById(R.id.btncancel)
            val savebtn: MaterialButton = dialogView.findViewById(R.id.btnsave)
            val imagename: EditText = dialogView.findViewById(R.id.imagename)


            val image: ImageView = dialogView.findViewById(R.id.screenshotimage)
            val screenshot: Bitmap = takeScreenshot(arFragment)

            image.setImageBitmap(screenshot)


            val dialog = Dialog(view.context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
            val dialogWindow = dialog.window
            dialogWindow?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.WHITE))

            dialog.setContentView(dialogView)
            dialog.setCancelable(false)
            dialog.show()
            cancelbtn.setOnClickListener {
                binding.btnClear.visibility = View.VISIBLE
                binding.btnAdd.visibility = View.VISIBLE
                binding.floatingActionButton2.visibility = View.VISIBLE
                dialog.dismiss()

            }
            savebtn.setOnClickListener {
                if (imagename.text.isEmpty()) {
                    Toast.makeText(context, "Enter image name", Toast.LENGTH_SHORT).show()
                } else {
                    savedata(imagename.text.toString(), dialog)

                }
            }

        }

        initObjects()

        arFragment.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, motionEvent: MotionEvent ->

            refreshAim(hitResult, motionEvent)
        }
        binding.btnAdd.setOnClickListener {
            addFromAim()
        }
        arFragment.arSceneView.scene.addOnUpdateListener {

            touchScreenCenterConstantly()
            updateDistance()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)


    private fun takeScreenshot(arFragment: ArFragment): Bitmap {
        val view = arFragment.arSceneView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(view.width * view.height)
        val buffer = IntBuffer.wrap(pixels)

        PixelCopy.request(view, bitmap, { copyResult ->
            if (copyResult == PixelCopy.SUCCESS) {
                // Screenshot successfully copied
                // You can use the 'bitmap' here
            } else {
                // Failed to copy screenshot
            }
        }, Handler())

        return bitmap
    }

    private fun saveBitmapToUri(bitmap: Bitmap): Uri? {
        var fileOutputStream: FileOutputStream? = null
        var file: File? = null
        try {
            val imageDir = context?.getExternalFilesDir(null)
            file = File(imageDir, "${UUID.randomUUID()}.jpg")
            fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                fileOutputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return if (file != null) Uri.fromFile(file) else null
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun savedata(imagename: String, dialog: Dialog) {
        val progressDialog = ProgressDialog(context)
        progressDialog.setCancelable(false)
        progressDialog.setMessage("Please Wait...")
        progressDialog.show()
        val userid = auth?.currentUser?.uid

        val screenshot: Bitmap = takeScreenshot(arFragment)
        val imageUri: Uri? = saveBitmapToUri(screenshot)

        // Continue with further processing or save the URI to a variable
        if (imageUri != null) {
            d("CHAGAN", imageUri.toString())
            val currentTimeMillis = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val dateString = dateFormat.format(Date(currentTimeMillis))
            val currentdate = System.currentTimeMillis().toString()
            storageref.getReference(userid.toString())
                .child(imagename.toString())

                .putFile(imageUri).addOnSuccessListener { task ->
                    task.metadata?.reference?.downloadUrl
                        ?.addOnSuccessListener {
                            val imagemap = mapOf(
                                "url" to it.toString(),
                                "time" to dateString,
                                "name" to imagename
                            )
                            val databaseReference = FirebaseDatabase.getInstance()
                                .getReference(auth?.currentUser?.uid.toString())
                            databaseReference.child(imagename).setValue(imagemap)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        context,
                                        "Image Upload Successful",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    dialog.dismiss()
                                    progressDialog.dismiss()
                                    startActivity(Intent(context, MainActivity::class.java))

                                    binding.btnClear.visibility = View.VISIBLE
                                    binding.btnAdd.visibility = View.VISIBLE
                                    binding.floatingActionButton2.visibility = View.VISIBLE
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        context,
                                        "Image Upload Failed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    dialog.dismiss()
                                    progressDialog.dismiss()
                                    binding.btnClear.visibility = View.VISIBLE
                                    binding.btnAdd.visibility = View.VISIBLE
                                    binding.floatingActionButton2.visibility = View.VISIBLE
                                }

                        }
                        ?.addOnFailureListener {
                            Toast.makeText(context, "Image Upload Failed", Toast.LENGTH_SHORT)
                                .show()
                            startActivity(Intent(context, MainActivity::class.java))
                            progressDialog.dismiss()
                            binding.btnClear.visibility = View.VISIBLE
                            binding.btnAdd.visibility = View.VISIBLE
                            binding.floatingActionButton2.visibility = View.VISIBLE
                        }
                }.addOnFailureListener {
                    progressDialog.dismiss()

                    Toast.makeText(context, "Image Upload Failed", Toast.LENGTH_SHORT).show()
                    d("CHAGAN", it.message.toString())

                }


        }


    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun updateDistance() {

        anchorNodeTemp?.let {

            if (::lineBetween.isInitialized) {
                arFragment.arSceneView.scene.removeChild(lineBetween)
            }

            if (currentAnchorNode.size < 2) {
                node1Pos = initialAnchor?.worldPosition
                node2Pos = anchorNodeTemp?.worldPosition
            } else {
                node1Pos = currentAnchorNode[currentAnchorNode.size - 1].worldPosition
                node2Pos = anchorNodeTemp?.worldPosition
            }
            calculateDistance(node1Pos!!, node2Pos!!)

        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun calculateDistance(node1Pos: Vector3, node2Pos: Vector3) {
        difference = Vector3.subtract(node1Pos, node2Pos)
        totalLength += difference!!.length()
        val rotationFromAToB = Quaternion.lookRotation(
            difference!!.normalized(),
            Vector3.up()
        )
        //setting lines between points
        lineBetween = AnchorNode().apply {
            setParent(arFragment.arSceneView.scene)
            worldPosition = Vector3.add(node1Pos, node2Pos).scaled(.5f)
            worldRotation = rotationFromAToB
            localScale = Vector3(1f, 1f, difference!!.length())
            renderable = widthLineRender
        }
        //settinglabel
        if (lengthLabel == null) {
            lengthLabel = AnchorNode()
            lengthLabel!!.setParent(arFragment.arSceneView.scene)
        }
        lengthLabel!!.worldPosition = Vector3.add(node1Pos, node2Pos).scaled(.5f)
        initTextBoxes(difference!!.length(), lengthLabel!!, false)
    }


    @RequiresApi(Build.VERSION_CODES.N)
    private fun initObjects() {
        MaterialFactory.makeOpaqueWithColor(requireContext(), Color(rgb(219, 68, 55)))
            .thenAccept { material: Material? ->
                heightLineRender = ShapeFactory.makeCube(Vector3(.015f, 1f, 1f), zero(), material)
                heightLineRender!!.apply {
                    isShadowCaster = false
                    isShadowReceiver = false
                }
            }

        MaterialFactory.makeTransparentWithColor(requireContext(), Color(rgb(23, 107, 230)))
            .thenAccept { material: Material? ->
                pointRender = ShapeFactory.makeCylinder(0.02f, 0.0003f, zero(), material)
                pointRender!!.isShadowCaster = false
                pointRender!!.isShadowReceiver = false
            }

        ViewRenderable.builder()
            .setView(requireContext(), com.example.armeasur.R.layout.distance)
            .build()
            .thenAccept { renderable: ViewRenderable ->
                renderable.apply {
                    isShadowCaster = false
                    isShadowReceiver = false
                    verticalAlignment = ViewRenderable.VerticalAlignment.BOTTOM
                }
                viewRenderable = renderable
            }

        Texture.builder()
            .setSource(requireContext(), com.example.armeasur.R.drawable.aim)
            .build().thenAccept { texture ->
                MaterialFactory.makeTransparentWithTexture(requireContext(), texture)
                    .thenAccept { material: Material? ->
                        aimRender = ShapeFactory.makeCylinder(0.08f, 0f, zero(), material)
                        aimRender!!.isShadowCaster = false
                        aimRender!!.isShadowReceiver = false
                    }
            }
        MaterialFactory.makeOpaqueWithColor(requireContext(), Color(rgb(23, 107, 230)))
            .thenAccept { material: Material? ->
                widthLineRender = ShapeFactory.makeCube(Vector3(.01f, 0f, 1f), zero(), material)
                widthLineRender!!.apply {
                    isShadowCaster = false
                    isShadowReceiver = false
                }
            }
    }

    private fun refreshAim(hitResult: HitResult, motionEvent: MotionEvent) {
        if (motionEvent.metaState == 0) {
            if (anchorNodeTemp != null) {
                anchorNodeTemp!!.anchor!!.detach()
            }
            if (anchorNodeTemp == null) {
                initialAnchor = AnchorNode(hitResult.createAnchor())
                currentAnchorNode.add(initialAnchor!!)
                tempAnchorNodes.add(initialAnchor!!)
            }
            val anchor = hitResult.createAnchor()
            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(arFragment.arSceneView.scene)
            val transformableNode = TransformableNode(arFragment.transformationSystem)
            transformableNode.renderable = aimRender
            transformableNode.setParent(anchorNode)
            arFragment.arSceneView.scene.addChild(anchorNode)
            anchorNodeTemp = anchorNode

        }
    }

    // add points to the surface based on the crosshair position
    // add lines between points
    // add labels
    @RequiresApi(Build.VERSION_CODES.N)
    private fun addFromAim() {
        if (anchorNodeTemp != null) {

            tempAnchorNodes.add(anchorNodeTemp!!)


            val worldPosition = anchorNodeTemp!!.worldPosition
            val worldRotation = anchorNodeTemp!!.worldRotation


            // add point
            worldPosition.x += 0.0000001f
            val confirmedAnchorNode = AnchorNode()
            confirmedAnchorNode.worldPosition = worldPosition
            confirmedAnchorNode.worldRotation = worldRotation
            val anchor = confirmedAnchorNode.anchor
            confirmedAnchorNode.setParent(arFragment.arSceneView.scene)
            TransformableNode(arFragment.transformationSystem).apply {
                renderable = pointRender
                setParent(confirmedAnchorNode)
            }
            arFragment.arSceneView.scene.addChild(confirmedAnchorNode)
            currentAnchor.add(anchor)
            currentAnchorNode.add(confirmedAnchorNode)
            if (currentAnchorNode.size >= 2) {

                difference = Vector3.subtract(node1Pos, node2Pos)
                totalLength += difference!!.length()
                val rotationFromAToB =
                    Quaternion.lookRotation(difference!!.normalized(), Vector3.up())
                //setting lines between points
                AnchorNode().apply {
                    setParent(arFragment.arSceneView.scene)
                    this.worldPosition = Vector3.add(node1Pos, node2Pos).scaled(.5f)
                    this.worldRotation = rotationFromAToB
                    localScale = Vector3(1f, 1f, difference!!.length())
                    renderable = widthLineRender
                }
                //setting labels with distances
                labelArray.add(AnchorNode().apply {
                    setParent(arFragment.arSceneView.scene)
                    this.worldPosition = Vector3.add(node1Pos, node2Pos).scaled(.5f)
                    initTextBoxes(difference!!.length(), this, true)
                })
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun initTextBoxes(
        meters: Float,
        transformableNode: AnchorNode,
        isFromCreateNewAnchor: Boolean
    ) {

        if (isFromCreateNewAnchor) {
            ViewRenderable.builder()
                .setView(requireContext(), com.example.armeasur.R.layout.distance)
                .build()
                .thenAccept { renderable: ViewRenderable ->
                    renderable.apply {
                        isShadowCaster = false
                        isShadowReceiver = false
                        verticalAlignment = ViewRenderable.VerticalAlignment.BOTTOM
                    }

                    addDistanceCard(renderable, meters, transformableNode)


                }
        } else {
            addDistanceCard(viewRenderable, meters, transformableNode)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun addDistanceCard(
        distanceRenderable: ViewRenderable,
        meters: Float,
        transformableNode: AnchorNode
    ) {
        distanceInMeters = distanceRenderable.view as CardView
        val metersString: String = if (meters < 1f) {
            String.format(Locale.ENGLISH, "%.0f", meters * 100) + " cm"
        } else {
            String.format(Locale.ENGLISH, "%.2f", meters) + " m"
        }
        val tv = distanceInMeters.getChildAt(0) as TextView
        tv.text = metersString
        Log.e("meters", metersString)
        transformableNode.renderable = distanceRenderable
    }

    // imitate clicks to the center of the screen (to the crosshair)
    private fun touchScreenCenterConstantly() {
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis() + 10
        val x = this.resources.displayMetrics.widthPixels.toFloat() / 2
        val y = this.resources.displayMetrics.heightPixels.toFloat() / 2
        val motionEvent = MotionEvent.obtain(
            downTime,
            eventTime,
            MotionEvent.ACTION_UP,
            x,
            y,
            0
        )
        arFragment.arSceneView.dispatchTouchEvent(motionEvent)
    }


    // rotate labels according to camera movements
    private fun labelsRotation() {
        val cameraPosition = arFragment.arSceneView.scene.camera.worldPosition
        for (labelNode in labelArray) {
            val labelPosition = labelNode.worldPosition
            val direction = Vector3.subtract(cameraPosition, labelPosition)
            val lookRotation = Quaternion.lookRotation(direction, Vector3.up())
            labelNode.worldRotation = lookRotation
        }
    }

    fun clearAnchors() {
        for (i in currentAnchorNode) {
            arFragment.arSceneView.scene.removeChild(i)
        }
        currentAnchorNode.clear()
        currentAnchor.clear()
        labelArray.clear()
        totalLength = 0f

    }


    override fun onStart() {
        super.onStart()
        if (::arFragment.isInitialized) {
            arFragment.onStart()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::arFragment.isInitialized) {
            arFragment.onPause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::arFragment.isInitialized) {
            arFragment.onResume()
        }
    }

    @Deprecated("Deprecated in Java")

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val photo = data?.extras?.get("data") as? Bitmap

            // Convert Bitmap to byte array
            val baos = ByteArrayOutputStream()
            photo?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val imageData = baos.toByteArray()

            // Generate a unique image name
            val imageName = UUID.randomUUID().toString() + ".jpg"

            // Check if the user is authenticated

            // User is logged in
            val userId = auth?.currentUser!!.uid

            // Create a reference to the storage path where you want to store the image
            val storageRef = storage?.reference?.child(userId)?.child(imageName)

            // Upload the byte array to Firebase Storage
            val uploadTask = storageRef?.putBytes(imageData)
            uploadTask?.addOnSuccessListener { taskSnapshot ->
                // Image upload successful
                // You can retrieve the download URL of the image using taskSnapshot.metadata?.reference?.downloadUrl()
                // and save it to the Firebase Database if needed.
                // Example: val downloadUrl = taskSnapshot.metadata?.reference?.downloadUrl.toString()

                // Get the current date and time
                val currentDate = Date()
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentDate)
                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(currentDate)

                // Store the date and time separately in the Firebase Realtime Database
                val userDateRef = dateRef?.child(userId)?.push()
                val userTimeRef = timeRef?.child(userId)?.push()
                userDateRef?.setValue(date)
                userTimeRef?.setValue(time)

                // Perform further actions with the uploaded image, date, and time
                Toast.makeText(
                    context,
                    "Image uploaded successfully. Date: $date, Time: $time",
                    Toast.LENGTH_SHORT
                ).show()
            }?.addOnFailureListener {
                // Image upload failed
                Toast.makeText(context, "Image upload failed", Toast.LENGTH_SHORT).show()
            }
//            }
//            else {
//                // User is not logged in
//                Toast.makeText(context, "User is not logged in", Toast.LENGTH_SHORT).show()
//            }
        } else {
            Toast.makeText(context, "CANCELLED", Toast.LENGTH_SHORT).show()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}

