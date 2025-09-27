package com.aln.ultiwear.model

import com.aln.ultiwear.R

enum class Condition(val resId: Int) {
    NEW(R.string.condition_new),
    LIKE_NEW(R.string.condition_like_new),
    GOOD(R.string.condition_good),
    FAIR(R.string.condition_ok),
    POOR(R.string.condition_poor)
}

enum class Size {
    XS,
    S,
    M,
    L,
    XL,
    XXL
}

data class WardrobeItem(
    val id: String,
    val owner: String,
    val condition: Condition,
    val size: Size,
    val frontImageUrl: String,
    val backImageUrl: String?
)