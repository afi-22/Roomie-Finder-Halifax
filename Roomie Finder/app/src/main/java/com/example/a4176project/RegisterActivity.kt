package com.example.a4176project

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import androidx.appcompat.widget.Toolbar

class RegisterActivity : AppCompatActivity() {
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        database = FirebaseDatabase.getInstance()

        val usernameEditText: EditText = findViewById(R.id.editTextUsername)
        val emailEditText: EditText = findViewById(R.id.editTextEmail)
        val passwordEditText: EditText = findViewById(R.id.editTextPassword)
        val confirmPasswordEditText: EditText = findViewById(R.id.editTextConfirmPassword)
        val phoneEditText: EditText = findViewById(R.id.editTextPhone)
        val registerButton: Button = findViewById(R.id.buttonRegister)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.title = "Back"

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // First check if the username is unique
            database.reference.child("users").orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            Toast.makeText(this@RegisterActivity, "Username already exists", Toast.LENGTH_SHORT).show()
                        } else {
                            // If the username is unique, then check if the mailbox is unique
                            database.reference.child("users").orderByChild("email").equalTo(email)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(emailSnapshot: DataSnapshot) {
                                        if (emailSnapshot.exists()) {
                                            Toast.makeText(this@RegisterActivity, "Email already in use", Toast.LENGTH_SHORT).show()
                                        } else {
                                            // Username and email address are unique and can register new users
                                            val userRef = database.reference.child("users").push()
                                            val userData = hashMapOf(
                                                "username" to username,
                                                "email" to email,
                                                "address" to "",
                                                "password" to password,
                                                "phone" to phone,
                                                "bio" to "",
                                                "avatarUrl" to ""
                                            )

                                            userRef.setValue(userData)
                                                .addOnSuccessListener {
                                                    Toast.makeText(this@RegisterActivity, "Registration successful", Toast.LENGTH_SHORT).show()
                                                    finish()
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(this@RegisterActivity, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    }

                                    override fun onCancelled(databaseError: DatabaseError) {
                                        Toast.makeText(this@RegisterActivity, "Failed to check email availability: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                                    }
                                })
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@RegisterActivity, "Failed to check username availability: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}






