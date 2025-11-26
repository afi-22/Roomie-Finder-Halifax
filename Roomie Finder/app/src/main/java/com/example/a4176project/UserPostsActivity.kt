package com.example.a4176project

import android.app.Activity
import android.content.Intent
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
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.*

class UserPostsActivity : AppCompatActivity() {

    private lateinit var userId: String
    private lateinit var postRecyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private var postsList = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_posts)

        val backToolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(backToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Back"
        backToolbar.setNavigationOnClickListener {
            finish()
        }

        userId = intent.getStringExtra("userId") ?: ""

        postRecyclerView = findViewById(R.id.userPostsRecyclerView)
        initializeRecyclerView()

        loadPosts()
    }

    private fun initializeRecyclerView() {
        postAdapter = PostAdapter(this, postsList,
            onEdit = { post -> editPost(post) },
            onDelete = { post -> deletePost(post) },
            onPostClick = { post -> showPostDetailsPopup(post) }
        )

        postRecyclerView.layoutManager = LinearLayoutManager(this)
        postRecyclerView.adapter = postAdapter
    }

    private fun loadPosts() {
        val databasePosts = FirebaseDatabase.getInstance().reference.child("posts")
        databasePosts.orderByChild("userId").equalTo(userId).addListenerForSingleValueEvent(object : ValueEventListener {
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
                Log.w("UserPostsActivity", "loadPosts:onCancelled", databaseError.toException())
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


    override fun onResume() {
        super.onResume()
        loadPosts()
    }


    private fun showPostDetailsPopup(post: Post) {
        val inflater = layoutInflater
        val popupView = inflater.inflate(R.layout.popup_post_details, null)

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
            val chip = Chip(this@UserPostsActivity).apply {
                text = amenity
                isClickable = false
                isCheckable = false
            }
            amenitiesChipGroup.addView(chip)
        }




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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_POST_REQUEST && resultCode == Activity.RESULT_OK) {
            // Reload posts after editing
            loadPosts()
        }
    }

    companion object {
        const val EDIT_POST_REQUEST = 1
    }
}
