package com.aln.ultiwear.model

data class PostedWardrobeItem(
    val wardrobeItem: WardrobeItem,
    val post: Post?,
    val wardrobeUid: String
)