package com.mcc.signsaya.utils

fun String.maskEmail(): String {
    val parts = this.split("@")
    if (parts.size != 2) return this
    
    val name = parts[0]
    val domain = parts[1]
    
    return when {
        name.length <= 3 -> {
            // For very short names like "ab" or "abc", just show the first character
            name.take(1) + "*" + "@" + domain
        }
        else -> {
            // First 2 characters + stars + last 1 character
            val firstTwo = name.take(2)
            val lastOne = name.takeLast(1)
            val middleStars = "*".repeat(name.length - 3)
            "$firstTwo$middleStars$lastOne@$domain"
        }
    }
}
