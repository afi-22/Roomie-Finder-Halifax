package com.example.a4176project.search

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.a4176project.ChatActivity
import com.example.a4176project.Post
import com.example.a4176project.ProfileActivity
import com.example.a4176project.R
import com.example.a4176project.User
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class SearchAdapter(private val context: Context, private var postList: List<Post>, private val currentUserId: String) :
    RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    inner class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.postImage)
        val title: TextView = itemView.findViewById(R.id.postTitle)
    }

    fun setFilteredList(list: List<Post>) {
        this.postList = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.single_post, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val post = postList[position]

        if (!post.imageUrl.isNullOrEmpty()) {
            Glide.with(context)
                .load(post.imageUrl)
                .placeholder(R.drawable.default_avatar)
                .error(R.drawable.default_avatar)
                .into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.default_avatar)
        }

        holder.title.text = post.title

        holder.itemView.setOnClickListener {
            showPostDetailsPopup(post)
        }
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    private fun showPostDetailsPopup(post: Post) {
        val inflater = LayoutInflater.from(context)
        val popupView = inflater.inflate(R.layout.popup_post_details, null)

        val titleTextView = popupView.findViewById<TextView>(R.id.popupTitle)
        val contentTextView = popupView.findViewById<TextView>(R.id.popupContent)
        val amenitiesChipGroup = popupView.findViewById<ChipGroup>(R.id.popupAmenities_group)
        val locationTextView = popupView.findViewById<TextView>(R.id.popupLocation)
        val postImageView = popupView.findViewById<ImageView>(R.id.popupImage)
        val usernameTextView = popupView.findViewById<TextView>(R.id.popupUsername)
        val userAvatarImageView = popupView.findViewById<ImageView>(R.id.popupUserAvatar)
        val chatButton = popupView.findViewById<Button>(R.id.chatButton)

        titleTextView.text = post.title
        contentTextView.text = post.content

        //load amenities
        post.amenities?.forEach { amenity ->
            val chip = Chip(context).apply {
                text = amenity
                isClickable = false
                isCheckable = false
            }
            amenitiesChipGroup.addView(chip)
        }



        locationTextView.text = post.location

        Glide.with(context)
            .load(post.imageUrl)
            .placeholder(R.drawable.default_avatar)
            .error(R.drawable.default_avatar)
            .into(postImageView)

        post.userId?.let { userId ->
            FirebaseDatabase.getInstance().getReference("users").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        user?.let {
                            usernameTextView.text = it.username ?: "Unknown"

                            Glide.with(context)
                                .load(it.avatarUrl)
                                .placeholder(R.drawable.default_avatar)
                                .error(R.drawable.default_avatar)
                                .circleCrop()
                                .into(userAvatarImageView)


                            userAvatarImageView.setOnClickListener {
                                val profileIntent = Intent(context, ProfileActivity::class.java)
                                profileIntent.putExtra("userId", userId)
                                context.startActivity(profileIntent)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        usernameTextView.text = "Unknown"
                    }
                })
        } ?: run {
            usernameTextView.text = "Unknown"
            userAvatarImageView.setImageResource(R.drawable.default_avatar)
        }


        chatButton.setOnClickListener {
            post.userId?.let { userId ->
                val chatIntent = Intent(context, ChatActivity::class.java).apply {
                    putExtra("currentUserId", currentUserId) // 确保你已经有一个方式来获取或传递 currentUserId 到这里
                    putExtra("postUserId", userId)
                }
                context.startActivity(chatIntent)
            }
        }

        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        val layoutParams = (context as Activity).window.attributes
        layoutParams.alpha = 0.5f
        context.window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        context.window.attributes = layoutParams

        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)

        popupWindow.setOnDismissListener {
            layoutParams.alpha = 1f
            context.window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            context.window.attributes = layoutParams
        }
    }




}
