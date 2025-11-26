package com.example.a4176project

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
class PostAdapter(
    private val context: Context,
    private val postsList: MutableList<Post>,
    private val onEdit: (Post) -> Unit,
    private val onDelete: (Post) -> Unit,
    private val onPostClick: (Post) -> Unit,
    //flag to control button visibility
    private val isEditingEnabled: Boolean = true
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.post_item_layout, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postsList[position]
        with(holder) {
            titleTextView.text = post.title
            Glide.with(itemView.context)
                .load(post.imageUrl)
                .error(R.drawable.default_avatar)
                .into(postImageView)

            itemView.setOnClickListener { onPostClick(post) }
            editButton.setOnClickListener { onEdit(post) }
            deleteButton.setOnClickListener { onDelete(post) }

            // Set visibility based on the flag
            editButton.visibility = if (isEditingEnabled) View.VISIBLE else View.GONE
            deleteButton.visibility = if (isEditingEnabled) View.VISIBLE else View.GONE
        }
    }

    override fun getItemCount(): Int = postsList.size

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.postTitleTextView)
        val postImageView: ImageView = view.findViewById(R.id.postImageView)
        val editButton: Button = view.findViewById(R.id.editButton)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
    }
}


