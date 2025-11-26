package com.example.a4176project

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import java.util.concurrent.CountDownLatch

class ChatListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chatSessions: MutableList<ChatSession>
    private lateinit var adapter: ChatListAdapter
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        val backToolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(backToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Back"
        backToolbar.setNavigationOnClickListener {
            finish()
        }

        currentUserId = intent.getStringExtra("userId") ?: ""
        recyclerView = findViewById(R.id.recyclerViewChatList)
        chatSessions = mutableListOf()

        adapter = ChatListAdapter(chatSessions) { chatSession ->
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("currentUserId", currentUserId)
                putExtra("postUserId", chatSession.userId)
            }
            startActivity(intent)

        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadChatSessions()
    }

    private fun loadChatSessions() {
        chatSessions.clear() // Purge existing data to avoid duplication

        val messagesRef = FirebaseDatabase.getInstance().getReference("messages")
        val uniqueChatPartners = HashSet<String>()
        val chatSessionsMap = mutableMapOf<String, ChatSession>()

        messagesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { messageSnapshot ->
                    val message = messageSnapshot.getValue(Message::class.java)
                    message?.let {
                        if (it.senderId == currentUserId || it.receiverId == currentUserId) {
                            val partnerId = if (it.senderId == currentUserId) it.receiverId else it.senderId
                            uniqueChatPartners.add(partnerId)

                            // Update the last message
                            val existingSession = chatSessionsMap[partnerId]
                            if (existingSession == null || existingSession.timestamp < it.timestamp) {
                                chatSessionsMap[partnerId] = ChatSession(partnerId, "", it.message, it.timestamp)
                            }
                        }
                    }
                }

                if (uniqueChatPartners.isNotEmpty()) {
                    loadUsernamesForChatSessions(uniqueChatPartners, chatSessionsMap)
                } else {
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("ChatListActivity", "Failed to load messages", databaseError.toException())
            }
        })
    }

    private fun loadUsernamesForChatSessions(uniqueChatPartners: Set<String>, chatSessionsMap: MutableMap<String, ChatSession>) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        val countDownLatch = CountDownLatch(uniqueChatPartners.size)

        uniqueChatPartners.forEach { userId ->
            usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userName = snapshot.child("username").getValue(String::class.java) ?: "Unknown"
                    chatSessionsMap[userId]?.userName = userName

                    countDownLatch.countDown()
                }

                override fun onCancelled(error: DatabaseError) {
                    countDownLatch.countDown()
                }
            })
        }

        Thread {
            try {
                countDownLatch.await()
                runOnUiThread {
                    chatSessions.clear()
                    chatSessions.addAll(chatSessionsMap.values.sortedByDescending { it.timestamp })
                    adapter.notifyDataSetChanged()
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }.start()
    }


    override fun onResume() {
        super.onResume()
        loadChatSessions()
    }

    data class ChatSession(
        val userId: String,
        var userName: String,
        val lastMessage: String,
        val timestamp: Long,
        var unreadCount: Int = 0
    )


    class ChatListAdapter(private val chatSessions: List<ChatSession>, private val onClick: (ChatSession) -> Unit) :
        RecyclerView.Adapter<ChatListAdapter.ChatSessionViewHolder>() {

        class ChatSessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val userNameTextView: TextView = itemView.findViewById(R.id.tvUserName)
            val lastMessageTextView: TextView = itemView.findViewById(R.id.tvLastMessage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatSessionViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_session_item, parent, false)
            return ChatSessionViewHolder(view)
        }

        override fun onBindViewHolder(holder: ChatSessionViewHolder, position: Int) {
            val chatSession = chatSessions[position]
            holder.userNameTextView.text = chatSession.userName
            holder.lastMessageTextView.text = chatSession.lastMessage
            holder.itemView.setOnClickListener { onClick(chatSession) }
        }

        override fun getItemCount(): Int = chatSessions.size
    }

    data class Message(
        val senderId: String = "",
        val receiverId: String = "",
        val message: String = "",
        val timestamp: Long = 0L
    ) {
        constructor() : this("", "", "", 0L)
    }
}
