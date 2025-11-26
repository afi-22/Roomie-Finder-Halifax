package com.example.a4176project

data class Post(
    var id: String? = null,
    var title: String? = "",
    var content: String? = "",
    var amenities:List<String>? = null,
    var location: String? = "",
    var userId: String? = null,
    var imageUrl: String? = null,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0
)

