package com.example.a4176project


import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
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

class ProfileActivity : AppCompatActivity() {

    // Define Firebase references
    private lateinit var database: FirebaseDatabase
    private lateinit var usersRef: DatabaseReference

    // Define UI components
    private lateinit var profileUserName: TextView
    private lateinit var profileAvatar: ImageView
    private lateinit var emailDisplay: TextView
    private lateinit var phoneDisplay: TextView
    private lateinit var profileBio: TextView
    private lateinit var profileLocationText: TextView
    private lateinit var preferencesChipGroup: ChipGroup
    private lateinit var lifestyleChipGroup: ChipGroup


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val backToolbar = findViewById<Toolbar>(R.id.backToolbar)
        setSupportActionBar(backToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Back"
        backToolbar.setNavigationOnClickListener {
            finish()
        }
        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        usersRef = database.getReference("users")

        // Initialize UI components
        profileUserName = findViewById(R.id.profile_UserName)
        profileAvatar = findViewById(R.id.profile_avatar)
        emailDisplay = findViewById(R.id.email_display)
        phoneDisplay = findViewById(R.id.phone_display)
        profileBio = findViewById(R.id.profile_bio)
        profileLocationText = findViewById(R.id.profile_location_text)
        preferencesChipGroup = findViewById(R.id.profile_preferences_group)
        lifestyleChipGroup = findViewById(R.id.profile_lifestyle_group)

        // Get userId from intent
        val userId = intent.getStringExtra("userId") ?: return

        // Load user profile
        loadUserProfile(userId)
    }

    private fun loadUserProfile(userId: String) {
        usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                user?.let {
                    // Update UI with user info
                    profileUserName.text = it.username
                    emailDisplay.text = it.email
                    phoneDisplay.text = it.phone
                    profileBio.text = it.bio
                    profileLocationText.text = it.address
                    //load preferences
                    it.preferences?.forEach { preference ->
                        val chip = Chip(this@ProfileActivity).apply {
                            text = preference
                            isClickable = false
                            isCheckable = false
                        }
                        preferencesChipGroup.addView(chip)
                    }
                    //load lifestyle
                    it.lifestyles?.forEach { lifestyleItem ->
                        val chip = Chip(this@ProfileActivity).apply {
                            text = lifestyleItem
                            isClickable = false
                            isCheckable = false
                        }
                        lifestyleChipGroup.addView(chip)
                    }

                    // Use Glide to load the user avatar
                    Glide.with(this@ProfileActivity)
                        .load(it.avatarUrl)
                        .placeholder(R.drawable.default_avatar) // Provide a default avatar image
                        .error(R.drawable.default_avatar) // Use default image in case of error
                        .into(profileAvatar)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible database errors
            }
        })
    }
}
