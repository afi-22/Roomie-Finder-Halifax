package com.example.a4176project

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class PostActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
   //amenities
    private lateinit var amenities: Array<String>
    private lateinit var amenitiesChipGroup: ChipGroup
    private lateinit var selectedAmenities: BooleanArray
    private lateinit var buttonSelectAmenities: Button
    private lateinit var locationEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var selectImageButton: Button
    private lateinit var postImageView: ImageView
    private var postId: String? = null
    private var userId: String? = null
    private var selectedImageUri: Uri? = null
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    companion object {
        private const val TAG = "PostActivity"
        private const val REQUEST_CODE_IMAGE_PICK = 1001
        private const val REQUEST_CODE_SELECT_LOCATION = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()
        titleEditText = findViewById(R.id.editTextPostTitle)
        contentEditText = findViewById(R.id.editTextPostContent)

        //amenities
        amenities = resources.getStringArray(R.array.amenities)
        selectedAmenities = BooleanArray(amenities.size) { false }

        buttonSelectAmenities = findViewById(R.id.button_select_amenities)
        locationEditText = findViewById(R.id.editTextLocation)
        saveButton = findViewById(R.id.buttonSavePost)
        selectImageButton = findViewById(R.id.buttonSelectImage)
        postImageView = findViewById(R.id.imageViewPost)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.title = "Back"

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)



        userId = intent.getStringExtra("userId")
        if (userId.isNullOrEmpty()) {
            Log.e(TAG, "User ID is null or empty.")
            Toast.makeText(this, "User ID is null or empty", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        postId = intent.getStringExtra("POST_ID")
        if (!postId.isNullOrEmpty()) {
            loadPostData(postId!!)
        }

        saveButton.setOnClickListener {
            if (userId.isNullOrEmpty()) {
                Log.e(TAG, "User ID is null or empty.")
                Toast.makeText(this, "User ID is null or empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (postId.isNullOrEmpty()) {
                createNewPost()
            } else {
                updatePost(postId!!)
            }
        }

        selectImageButton.setOnClickListener {
            selectImage()
        }

        // Add a click listener for the Select Position button.
        val selectLocationButton: Button = findViewById(R.id.buttonSelectLocation)
        selectLocationButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_SELECT_LOCATION)
        }

        //amenities pop up checkbox
        buttonSelectAmenities.setOnClickListener {
            //alertDialog
            AlertDialog.Builder(this).apply {
                setTitle("Select Amenities")
                setMultiChoiceItems(amenities, selectedAmenities) { _, which, isChecked ->
                    selectedAmenities[which] = isChecked
                }
                setPositiveButton("OK") { dialog, which ->
                    val selectedStrings = amenities.indices.filter { selectedAmenities[it] }.map { amenities[it] }
                    Toast.makeText(applicationContext, "Selected Preferences: $selectedStrings", Toast.LENGTH_LONG).show()
                }
                //temporary display
                show()
            }
        }



    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            postImageView.setImageURI(selectedImageUri)
        } else if (requestCode == REQUEST_CODE_SELECT_LOCATION && resultCode == Activity.RESULT_OK) {
            // Get the latitude and longitude information returned by the map activity
            latitude = data?.getDoubleExtra("latitude", 0.0) ?: 0.0
            longitude = data?.getDoubleExtra("longitude", 0.0) ?: 0.0
            val address = data?.getStringExtra("address")
            locationEditText.setText(address)
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_IMAGE_PICK)
    }

    private fun createNewPost() {
        val title = titleEditText.text.toString().trim()
        val content = contentEditText.text.toString().trim()
        // extract selected amenities into a list
        val selectedAmenitiesList = amenities.indices
            .filter { selectedAmenities[it] }
            .map { amenities[it] }

        val location = locationEditText.text.toString().trim()

        if (selectedImageUri == null) {
            // Save the post without selecting an image
            savePost(title, content, selectedAmenitiesList, location, "")
        } else {
            // Have a selection of images to upload first
            uploadImageToFirebase(selectedImageUri) { imageUrl ->
                savePost(title, content, selectedAmenitiesList, location, imageUrl)
            }
        }
    }

    private fun savePost(title: String, content: String, amenities: List<String>, location: String, imageUrl: String) {
        val newPostId = database.child("posts").push().key ?: return
        val newPost = Post(newPostId, title, content, amenities, location, userId!!, imageUrl, latitude, longitude)
        database.child("posts").child(newPostId).setValue(newPost).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Post created successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Log.e(TAG, "Failed to create post: ${task.exception?.message}")
                Toast.makeText(this, "Failed to create post", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePost(postId: String) {
        val title = titleEditText.text.toString().trim()
        val content = contentEditText.text.toString().trim()

        // extract selected amenities into a list
        buttonSelectAmenities.setOnClickListener {
            //alertDialog
            AlertDialog.Builder(this).apply {
                setTitle("Select Amenities")
                setMultiChoiceItems(amenities, selectedAmenities) { _, which, isChecked ->
                    selectedAmenities[which] = isChecked
                }
                setPositiveButton("OK") { dialog, which ->
                    val selectedStrings = amenities.indices.filter { selectedAmenities[it] }.map { amenities[it] }
                    Toast.makeText(applicationContext, "Selected Preferences: $selectedStrings", Toast.LENGTH_LONG).show()
                }
                //temporary display
                show()
            }
        }
        val selectedAmenitiesList = amenities.indices
            .filter { selectedAmenities[it] }
            .map { amenities[it] }



        val location = locationEditText.text.toString().trim()

        if (selectedImageUri == null) {
            // No new images, just update the post
            val postUpdateMap = mapOf(
                "title" to title,
                "content" to content,
                "amenities" to selectedAmenitiesList,
                "location" to location,
                "userId" to userId!!,
                "latitude" to latitude,
                "longitude" to longitude
            )
            updatePostInDatabase(postId, postUpdateMap)
        } else {

            uploadImageToFirebase(selectedImageUri) { imageUrl ->
                val postUpdateMap = mapOf(
                    "title" to title,
                    "content" to content,
                    "amenities" to selectedAmenitiesList,
                    "location" to location,
                    "userId" to userId!!,
                    "imageUrl" to imageUrl,
                    "latitude" to latitude,
                    "longitude" to longitude
                )
                updatePostInDatabase(postId, postUpdateMap)
            }
        }
    }

    private fun updatePostInDatabase(postId: String, postUpdateMap: Map<String, Any>) {
        database.child("posts").child(postId).updateChildren(postUpdateMap).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Post updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Log.e(TAG, "Failed to update post: ${task.exception?.message}")
                Toast.makeText(this, "Failed to update post", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadImageToFirebase(imageUri: Uri?, onSuccess: (String) -> Unit) {
        if (imageUri == null) return
        val filename = UUID.randomUUID().toString()
        val ref = storage.getReference("/images/$filename")

        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    Glide.with(this /* context */)
                        .load(imageUrl)
                        .into(postImageView)
                    onSuccess(imageUrl)
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to upload image: ${it.message}")
                Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPostData(postId: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("posts")
        databaseReference.child(postId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val post = snapshot.getValue(Post::class.java)
                    if (post != null) {
                        titleEditText.setText(post.title)
                        contentEditText.setText(post.content)
                        locationEditText.setText(post.location)

                        //load and display amenities in the checkbox
                        buttonSelectAmenities.setOnClickListener{
                            displayAmenitiesDialog()
                        }



                        Glide.with(this@PostActivity)
                            .load(post.imageUrl)
                            .error(R.drawable.default_avatar)
                            .into(postImageView)

                        latitude = post.latitude
                        longitude = post.longitude
                    } else {
                        Log.e(TAG, "Failed to parse post data.")
                        Toast.makeText(applicationContext, "Failed to parse post data", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "Post data does not exist.")
                    Toast.makeText(applicationContext, "Post data does not exist", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load post data: ${error.message}")
                Toast.makeText(applicationContext, "Failed to load post data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayAmenitiesDialog(){
        AlertDialog.Builder(this).apply {
            setTitle("Select Amenities")
            setMultiChoiceItems(amenities, selectedAmenities) { _, which, isChecked ->
                // update the selected amenities
                selectedAmenities[which] = isChecked
            }
            setPositiveButton("OK") { dialog, _ ->
                val selectedAmenitiesList = amenities.indices
                    .filter { selectedAmenities[it] }
                    .map { amenities[it] }
                Toast.makeText(applicationContext, "Selected Amenities: $selectedAmenitiesList", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            show()
        }
    }


    }
    private fun setSpinnerToValue(spinner: Spinner, value: String?) {
        val adapter = spinner.adapter
        for (position in 0 until adapter.count) {
            if (adapter.getItem(position).toString().equals(value, ignoreCase = true)) {
                spinner.setSelection(position)
                return
            }
        }
    }

