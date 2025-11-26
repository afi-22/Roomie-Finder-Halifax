package com.example.a4176project

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.database.*

class LoginActivity : AppCompatActivity() {
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        database = FirebaseDatabase.getInstance()

        val emailEditText: EditText = findViewById(R.id.editTextEmail)
        val passwordEditText: EditText = findViewById(R.id.editTextPassword)
        val loginButton: Button = findViewById(R.id.buttonLogin)
        val registerButton: Button = findViewById(R.id.buttonRegister)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.title = "Back"

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Query for matching user information
            val usersRef = database.reference.child("users")
            usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val userData = userSnapshot.getValue(User::class.java)
                            if (userData?.password == password) {
                                Toast.makeText(this@LoginActivity, "Login successful", Toast.LENGTH_SHORT).show()

                                val userId = userSnapshot.key // get ID
                                val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                                intent.putExtra("username", userData.username)
                                intent.putExtra("userId", userId) // Passing the user ID to the DashboardActivity
                                startActivity(intent)
                                return
                            }


                        }
                        // password doesn't match
                        Toast.makeText(this@LoginActivity, "Incorrect password", Toast.LENGTH_SHORT).show()
                    } else {
                        // user doesn't exit
                        Toast.makeText(this@LoginActivity, "User does not exist", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Inquiry Canceled
                    Toast.makeText(this@LoginActivity, "Database query cancelled", Toast.LENGTH_SHORT).show()
                }
            })
        }

        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

}

data class User(
    val username: String? = "",
    val email: String? = "",
    val password: String? = "",
    val phone: String? = "",
    val bio: String = "",
    val address: String? = "",
    val avatarUrl: String? = "",
    val preferences: List<String>? = null,
    val lifestyles: List<String>? = null
) {
    constructor() : this("", "", "", "", "", "")
}

