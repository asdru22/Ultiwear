package com.aln.ultiwear.model

import com.aln.ultiwear.R
import com.google.firebase.firestore.Exclude

enum class Condition(val resId: Int) {
    NEW(R.string.condition_new),
    LIKE_NEW(R.string.condition_like_new),
    GOOD(R.string.condition_good),
    OK(R.string.condition_ok),
    POOR(R.string.condition_poor)
}

enum class Size {
    XS, S, M, L, XL, XXL
}

data class WardrobeItem(
    val id: String = "",
    val owner: String = "",
    val conditionStr: String = "",
    val sizeStr: String = "",
    val frontImageUrl: String = "",
    val backImageUrl: String? = null,
    val tradeable: Boolean = false,
    val posted: Boolean = false
) {
    // ignored by firebase
    @get:Exclude
    val condition: Condition
        get() = Condition.entries.find { it.name == conditionStr } ?: Condition.NEW

    @get:Exclude
    val size: Size
        get() = Size.entries.find { it.name == sizeStr } ?: Size.M
}

