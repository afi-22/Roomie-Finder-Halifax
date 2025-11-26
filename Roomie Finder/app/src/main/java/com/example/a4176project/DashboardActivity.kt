package com.example.a4176project

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.a4176project.search.SearchActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private lateinit var databasePosts: DatabaseReference
    private lateinit var postRecyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var textViewUsername: TextView // Declare as member variable
    private lateinit var imageViewAvatar: ImageView // Declare as member variable
    private var postsList = mutableListOf<Post>()
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        database = FirebaseDatabase.getInstance()
        imageViewAvatar = findViewById(R.id.imageViewAvatar)
        textViewUsername = findViewById(R.id.textViewUsername)
        postRecyclerView = findViewById(R.id.postsRecyclerView)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        userId = intent.getStringExtra("userId") ?: ""

        val textViewLogout: TextView = findViewById(R.id.textViewLogout)
        textViewLogout.paintFlags = textViewLogout.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        imageViewAvatar.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java).apply {
                putExtra("userId", userId)
            }
            startActivity(intent)
        }

        textViewLogout.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        if (userId.isNotEmpty()) {
            loadUserInfo()
        }

        setupBottomNavigationView()
        initializeRecyclerView()
        loadPosts(userId)

        listenForNewMessages(userId)
    }

    private fun loadUserInfo() {
        database.reference.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    textViewUsername.text = "Hello, ${user?.username ?: "Anonymous"}"
                    Glide.with(this@DashboardActivity)
                        .load(user?.avatarUrl)
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .into(imageViewAvatar)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("DashboardActivity", "Failed to read user", error.toException())
                    textViewUsername.text = "Hello, Anonymous"
                }
            })
    }


    private fun setupBottomNavigationView() {
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> true
                R.id.navigation_search -> {
                    startActivity(Intent(this, SearchActivity::class.java).apply { putExtra("userId", userId) })
                    true
                }
                R.id.navigation_edit_post -> {
                    startActivity(Intent(this, UserPostsActivity::class.java).apply { putExtra("userId", userId) })
                    true
                }

                R.id.navigation_create_post -> {
                    startActivity(Intent(this, PostActivity::class.java).apply { putExtra("userId", userId) })
                    true
                }
                R.id.navigation_message -> {
                    newMessageCount = 0
                    startActivity(Intent(this, ChatListActivity::class.java).apply { putExtra("userId", userId) })
                    true
                }
                else -> false
            }
        }
    }



    override fun onResume() {
        super.onResume()
        // Default select dashboard and load user info and posts
        bottomNavigationView.selectedItemId = R.id.navigation_dashboard
        if (userId.isNotEmpty()) {
            loadUserInfo()
            loadPosts(userId)
        }
    }


    private fun initializeRecyclerView() {
        postAdapter = PostAdapter(this,
            postsList,
            onEdit = { post -> editPost(post) },
            onDelete = { post -> deletePost(post) },
            onPostClick = { post ->
                //clicking on a post, show post details
                showPostDetailsPopup(post)
            }
        )


        postRecyclerView.layoutManager = LinearLayoutManager(this)
        postRecyclerView.adapter = postAdapter
    }
    private fun showPostDetailsPopup(post: Post) {
        // Implementation of a popup window showing post details
        val inflater = layoutInflater
        val popupView = inflater.inflate(R.layout.popup_post_details, null)
        val usernameTextView = popupView.findViewById<TextView>(R.id.popupUsername)
        val titleTextView = popupView.findViewById<TextView>(R.id.popupTitle)
        val contentTextView = popupView.findViewById<TextView>(R.id.popupContent)
        val amenitiesChipGroup = popupView.findViewById<ChipGroup>(R.id.popupAmenities_group)
        val locationTextView = popupView.findViewById<TextView>(R.id.popupLocation)
        val postImageView = popupView.findViewById<ImageView>(R.id.popupImage)

        val chatButton: Button = popupView.findViewById(R.id.chatButton)
        chatButton.visibility = View.GONE


        titleTextView.text = post.title
        contentTextView.text = post.content

        //load amenities
        post.amenities?.forEach { amenity ->
            val chip = Chip(this@DashboardActivity).apply {
                text = amenity
                isClickable = false
                isCheckable = false
            }
            amenitiesChipGroup.addView(chip)
        }

        //display username
        post.userId?.let { userId ->
            FirebaseDatabase.getInstance().getReference("users").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        user?.let {
                            usernameTextView.text = it.username ?: "Unknown"}}

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }
                })}

        locationTextView.text = post.location

        Glide.with(this)
            .load(post.imageUrl)
            .error(R.drawable.default_avatar) // Failed to load image
            .into(postImageView)


        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels

        val popupWidth = (screenWidth * 0.9).toInt()

        val popupWindow = PopupWindow(popupView, popupWidth, ViewGroup.LayoutParams.WRAP_CONTENT, true)

        val layoutParams = window.attributes
        layoutParams.alpha = 0.5f // Set the background transparency to 50%.
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.attributes = layoutParams


        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)

        popupWindow.setOnDismissListener {
            layoutParams.alpha = 1f
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.attributes = layoutParams
        }
    }




    // Modify loadPosts to accept userId as a parameter in order to load only the current user's posts
    private fun loadPosts(userId: String) {
        databasePosts = database.reference.child("posts")
        databasePosts.orderByChild("userId").equalTo(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postsList.clear()
                if (snapshot.exists()) {
                    for (postSnapshot in snapshot.children) {
                        val post = postSnapshot.getValue(Post::class.java)
                        post?.id = postSnapshot.key
                        post?.let { postsList.add(it) }
                    }
                    postAdapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Log the error
                Log.w("DashboardActivity", "loadPost:onCancelled", databaseError.toException())
                // Show an error message to the user
                Toast.makeText(baseContext, "Failed to load posts.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun editPost(post: Post) {
        val editIntent = Intent(this, PostActivity::class.java).apply {
            putExtra("POST_ID", post.id)
            putExtra("userId", userId)
        }
        startActivity(editIntent)
    }

    private fun deletePost(post: Post) {
        AlertDialog.Builder(this)
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Yes") { dialog, which ->
                val databaseReference = FirebaseDatabase.getInstance().getReference("posts")
                post.id?.let {
                    databaseReference.child(it).removeValue().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            postsList.remove(post)
                            postAdapter.notifyDataSetChanged()
                            Toast.makeText(this, "Post deleted successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed to delete post", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
    private var newMessageCount: Int = 0

    private fun listenForNewMessages(userId: String) {
        val ref = FirebaseDatabase.getInstance().getReference("/messages")
        ref.orderByChild("receiverId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var unreadCount = 0
                    for (ds in snapshot.children) {
                        val message = ds.getValue(ChatActivity.Message::class.java)
                        if (message != null && !message.isRead) {
                            unreadCount++
                        }
                    }
                    updateNavigationMessageBadge(unreadCount)
                }

                override fun onCancelled(error: DatabaseError) {
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Error reading messages", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }




    private fun updateNavigationMessageBadge(unreadCount: Int) {
        if (unreadCount > 0) {
            val badge = bottomNavigationView.getOrCreateBadge(R.id.navigation_message)
            badge.isVisible = true
            badge.number = unreadCount
        } else {
            bottomNavigationView.removeBadge(R.id.navigation_message)
        }
    }




}
