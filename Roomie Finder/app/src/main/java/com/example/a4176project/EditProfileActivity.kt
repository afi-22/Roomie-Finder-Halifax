package com.example.a4176project

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class EditProfileActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private lateinit var usersRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var userID: String
    private var selectedImageUri: Uri? = null
    private lateinit var imageViewAvatar: ImageView
    private lateinit var editTextAddress: EditText
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    //preferences list
    private lateinit var preferences: Array<String>
    private lateinit var selectedPreferences: BooleanArray
    //lifestyle list
    private lateinit var lifestyles: Array<String>
    private lateinit var selectedLifestyles: BooleanArray

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val LOCATION_PICKER_REQUEST = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        database = FirebaseDatabase.getInstance()
        usersRef = database.getReference("users")
        auth = FirebaseAuth.getInstance()

        userID = intent.getStringExtra("userId") ?: ""

        val editTextPhone: EditText = findViewById(R.id.editProfile_userPhone)
        val editTextUsername: EditText = findViewById(R.id.editProfile_userName)
        val editTextBio: EditText = findViewById(R.id.editProfile_bio)
        editTextAddress = findViewById(R.id.editText_address)
        imageViewAvatar = findViewById(R.id.editProfile_avatar)
        val saveButton: Button = findViewById(R.id.profile_save_changes_button)
        val uploadImageButton: Button = findViewById(R.id.editProfile_upload_img_button)
        val selectAddressButton: Button = findViewById(R.id.button_select_address)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.title = "Back"

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        loadUserData(editTextPhone, editTextUsername, editTextBio)

        saveButton.setOnClickListener {
            updateUserProfile(editTextPhone, editTextUsername, editTextBio)
        }

        uploadImageButton.setOnClickListener {
            chooseImage(it)
        }

        selectAddressButton.setOnClickListener {
            // Launch MapsActivity to select an address
            val intent = Intent(this, MapsActivity::class.java)
            startActivityForResult(intent, LOCATION_PICKER_REQUEST)
        }


        //select preferences pop-up checkbox
        // initialize the items for selection
        preferences = resources.getStringArray(R.array.preferences)
        selectedPreferences = BooleanArray(preferences.size) { false }

        val buttonSelectPreferences = findViewById<Button>(R.id.button_select_preference)

        buttonSelectPreferences.setOnClickListener {
            //alertDialog
            AlertDialog.Builder(this).apply {
                setTitle("Select Preferences")
                setMultiChoiceItems(preferences, selectedPreferences) { _, which, isChecked ->
                    selectedPreferences[which] = isChecked
                }
                setPositiveButton("OK") { dialog, which ->
                    val selectedStrings = preferences.indices.filter { selectedPreferences[it] }.map { preferences[it] }
                    Toast.makeText(applicationContext, "Selected Preferences: $selectedStrings", Toast.LENGTH_LONG).show()
                }
                //temporary display
                show()
            }
        }


        //select lifestyle pop-up checkbox
        // initialize the items for selection
        lifestyles = resources.getStringArray(R.array.lifestyles)
        selectedLifestyles = BooleanArray(preferences.size) { false }

        val buttonSelectLifestyles = findViewById<Button>(R.id.button_select_lifestyle)

        buttonSelectLifestyles.setOnClickListener {
            //alertDialog
            AlertDialog.Builder(this).apply {
                setTitle("Select Lifestyles")
                setMultiChoiceItems(lifestyles, selectedLifestyles) { _, which, isChecked ->
                    selectedLifestyles[which] = isChecked
                }
                setPositiveButton("OK") { dialog, which ->
                    val selectedStrings = lifestyles.indices.filter { selectedLifestyles[it] }.map { lifestyles[it] }
                    Toast.makeText(applicationContext, "Selected Lifestyles: $selectedStrings", Toast.LENGTH_LONG).show()
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

    private fun updateUserProfile(editTextPhone: EditText, editTextUsername: EditText, editTextBio: EditText) {
        val phone = editTextPhone.text.toString()
        val username = editTextUsername.text.toString()
        val bio = editTextBio.text.toString()
        val address = editTextAddress.text.toString()

        // extract selected preferences into a list
        val selectedPreferencesList = preferences.indices
            .filter { selectedPreferences[it] }
            .map { preferences[it] }

        //extract selected lifestyles into a list
        val selectedLifestyleList = preferences.indices
            .filter { selectedLifestyles[it] }
            .map { lifestyles[it] }

        val userUpdates = HashMap<String, Any>().apply {
            put("phone", phone)
            put("username", username)
            put("bio", bio)
            put("address", address)
            put("latitude", latitude)
            put("longitude", longitude)
            //add selected preferences to the updates
            put("preferences", selectedPreferencesList)
            //add selected lifestyles to the updates
            put("lifestyles", selectedLifestyleList)
        }

        if (selectedImageUri != null) {
            val storageRef = FirebaseStorage.getInstance().reference.child("avatars/$userID.jpg")
            storageRef.putFile(selectedImageUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        userUpdates["avatarUrl"] = uri.toString()
                        updateUserInDatabase(userUpdates)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to upload avatar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            updateUserInDatabase(userUpdates)
        }
    }

    private fun updateUserInDatabase(userUpdates: HashMap<String, Any>) {
        usersRef.child(userID).updateChildren(userUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    onBackPressed()
                } else {
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loadUserData(editTextPhone: EditText, editTextUsername: EditText, editTextBio: EditText) {
        usersRef.child(userID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                user?.let {
                    editTextPhone.setText(it.phone)
                    editTextUsername.setText(it.username)
                    editTextBio.setText(it.bio ?: "")
                    editTextAddress.setText(it.address ?: "")

                    Glide.with(this@EditProfileActivity)
                        .load(it.avatarUrl)
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .into(imageViewAvatar)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }



    fun chooseImage(view: View) {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            imageViewAvatar.setImageURI(selectedImageUri)
        } else if (requestCode == LOCATION_PICKER_REQUEST && resultCode == Activity.RESULT_OK && data != null) {

            val address = data.getStringExtra("address") ?: ""
            latitude = data.getDoubleExtra("latitude", 0.0)
            longitude = data.getDoubleExtra("longitude", 0.0)
            editTextAddress.setText(address)
        }
    }
}
