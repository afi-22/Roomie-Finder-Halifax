package com.example.a4176project.search

import android.os.Bundle
import android.view.Menu
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.a4176project.Post
import com.example.a4176project.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList
import java.util.EnumSet
import android.widget.PopupMenu
import android.widget.Toast
import android.app.AlertDialog



class SearchActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var adapter: SearchAdapter
    private lateinit var filterButton: ImageView
    private var postList = ArrayList<Post>()
    private lateinit var userId: String
    private var currentSelectedAmenity: String? = null
    private lateinit var selectedAmenities: BooleanArray
    private lateinit var amenities: Array<String>

    //filter option setting
    private var filterOptionSelected = -1

    //filtering by preferences
    private lateinit var preferences: Array<String>
    private lateinit var selectedPreferences: BooleanArray

    //filtering by lifestyles
    private lateinit var lifestyles: Array<String>
    private lateinit var selectedLifestyles: BooleanArray




    private val activeFilters = EnumSet.noneOf(FilterOption::class.java)

    // Filter options, hardcoded for now but should change depending on what the user selects
    private val locationValue = "Canada"
    private val preferenceValue = "Preference"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        filterButton = findViewById(R.id.filterButton)
        recyclerView = findViewById(R.id.searchRecycler)
        searchView = findViewById(R.id.searchView)
        userId = intent.getStringExtra("userId") ?: ""

        //initial preferences
        preferences = resources.getStringArray(R.array.preferences)
        selectedPreferences = BooleanArray(preferences.size) { false }

        //initial lifestyles
        lifestyles = resources.getStringArray(R.array.lifestyles)
        selectedLifestyles = BooleanArray(lifestyles.size) { false }



        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = SearchAdapter(this, postList, userId)
        recyclerView.adapter = adapter
        getPosts()

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.title = "Back"

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        amenities = resources.getStringArray(R.array.amenities)
        selectedAmenities = BooleanArray(amenities.size) { false }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText, currentSelectedAmenity)
                return true
            }
        })

        filterButton.setOnClickListener {

            showFilterOptionsDialog()

        }
    }

    //function to pop up filter choices
    private fun showFilterOptionsDialog() {
        val filterOptions = arrayOf("Amenities", "Preferences","Lifestyles")
        filterOptionSelected = -1

        AlertDialog.Builder(this).apply {
            setTitle("Filter by")
            setSingleChoiceItems(filterOptions, -1) { dialog, which ->
                filterOptionSelected = which
            }
            setPositiveButton("Select") { dialog, _ ->
                when (filterOptionSelected) {
                    0 -> showAmenitiesDialog()
                    1 -> showPreferencesDialog()
                    2 -> showLifeStyleDialog()
                }
                dialog.dismiss()
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        }.create().show()
    }


    private fun showAmenitiesDialog() {
        AlertDialog.Builder(this).apply {
            setTitle("Select Amenities")
            setMultiChoiceItems(amenities, selectedAmenities) { _, which, isChecked ->
                selectedAmenities[which] = isChecked
            }
            setPositiveButton("OK") { dialog, _ ->
                filterListBasedOnSelection()
                dialog.dismiss()
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        }.create().show()
    }

    private fun showPreferencesDialog() {
        AlertDialog.Builder(this).apply {
            setTitle("Select Preferences")
            setMultiChoiceItems(preferences, selectedPreferences) { _, which, isChecked ->
                selectedPreferences[which] = isChecked
            }
            setPositiveButton("OK") { dialog, _ ->
                filterListBasedOnSelection()
                dialog.dismiss()
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        }.create().show()
    }

    private fun showLifeStyleDialog() {
        AlertDialog.Builder(this).apply {
            setTitle("Select Preferences")
            setMultiChoiceItems(lifestyles, selectedLifestyles) { _, which, isChecked ->
                selectedLifestyles[which] = isChecked
            }
            setPositiveButton("OK") { dialog, _ ->
                filterListBasedOnSelection()
                dialog.dismiss()
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        }.create().show()
    }


    private fun filterListBasedOnSelection() {
        val filteredList = ArrayList<Post>()

        val selectedAmenitiesList = amenities.filterIndexed { index, _ ->
            selectedAmenities[index]
        }

        postList.forEach { post ->
            if (selectedAmenitiesList.all { post.amenities?.contains(it) == true }) {
                filteredList.add(post)
            }
        }

        adapter.setFilteredList(filteredList)
    }



    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun getPosts() {
        val databaseReference = FirebaseDatabase.getInstance().getReference("posts")

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                postList.clear()
                for (snapshot in dataSnapshot.children) {
                    val post = snapshot.getValue(Post::class.java)
                    post?.let { postList.add(it) }
                }
                filterList(searchView.query.toString(), currentSelectedAmenity)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // error
            }
        })
    }

    private fun filterList(query: String?, amenity: String?) {
        val filteredList = ArrayList<Post>()
        for (post in postList) {
            val matchesAmenity = amenity == null || post.amenities?.contains(amenity) == true
            val matchesQuery = query == null || query.isEmpty() || post.title?.contains(query, ignoreCase = true) == true || post.content?.contains(query, ignoreCase = true) == true

            if (matchesAmenity && matchesQuery) {
                filteredList.add(post)
            }
        }

        adapter.setFilteredList(filteredList)
    }


}


