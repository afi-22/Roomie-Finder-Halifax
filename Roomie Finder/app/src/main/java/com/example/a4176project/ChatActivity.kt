package com.example.a4176project

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.content.Context
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ChatActivity : AppCompatActivity() {

    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private val messages: MutableList<Message> = mutableListOf()

    private lateinit var currentUserId: String
    private lateinit var postUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val backToolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(backToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Back"
        backToolbar.setNavigationOnClickListener {
            finish()
        }

        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        chatRecyclerView = findViewById(R.id.chatRecyclerView)

        currentUserId = intent.getStringExtra("currentUserId") ?: ""
        postUserId = intent.getStringExtra("postUserId") ?: ""

        messageAdapter = MessageAdapter(messages, currentUserId, this)
        chatRecyclerView.adapter = messageAdapter
        chatRecyclerView.layoutManager = LinearLayoutManager(this)

        sendButton.setOnClickListener {
            sendMessage()
        }

        loadMessages()
    }

    private fun sendMessage() {
        val messageText = messageEditText.text.toString()
        if (messageText.isNotEmpty()) {
            val messageId = FirebaseDatabase.getInstance().reference.push().key
            val message = Message(currentUserId, postUserId, messageText, System.currentTimeMillis())
            val newMessagesRef = FirebaseDatabase.getInstance().getReference("new_messages")
            newMessagesRef.child(postUserId).setValue(true)

            messageId?.let {
                FirebaseDatabase.getInstance().reference
                    .child("messages")
                    .child(it)
                    .setValue(message)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            messageEditText.setText("")
                        }
                    }
            }
        }
    }

    private fun loadMessages() {
        val databaseReference = FirebaseDatabase.getInstance().getReference("/messages")
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messages.clear()
                val unreadMessagesIds = mutableListOf<String>()
                for (postSnapshot in snapshot.children) {
                    val message = postSnapshot.getValue(Message::class.java)
                    if (message != null && (message.senderId == currentUserId && message.receiverId == postUserId || message.senderId == postUserId && message.receiverId == currentUserId)) {
                        messages.add(message)
                        if (message.receiverId == currentUserId && !message.isRead) {
                            unreadMessagesIds.add(postSnapshot.key!!)
                        }
                    }
                }
                messages.sortBy { it.timestamp }
                messageAdapter.notifyDataSetChanged()
                chatRecyclerView.scrollToPosition(messages.size - 1)
                markMessagesAsRead(unreadMessagesIds)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle possible errors.
            }
        })
    }

    private fun markMessagesAsRead(messageIds: List<String>) {
        messageIds.forEach { messageId ->
            FirebaseDatabase.getInstance().getReference("/messages/$messageId/isRead").setValue(true)
        }
    }


    data class Message(
        val senderId: String = "",
        val receiverId: String = "",
        val message: String = "",
        val timestamp: Long = 0L,
        val isRead: Boolean = false
    )


    class MessageAdapter(private val messages: List<Message>, private val currentUserId: String, private val context: Context) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

        class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
            return MessageViewHolder(view)
        }

        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            val message = messages[position]
            FirebaseDatabase.getInstance().getReference("users/${message.senderId}").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    val username = user?.username ?: "Unknown"
                    holder.messageTextView.text = "$username: ${message.message}"
                }

                override fun onCancelled(error: DatabaseError) {
                    holder.messageTextView.text = "Error: ${error.message}"
                }
            })
        }

        override fun getItemCount(): Int = messages.size
    }
}
