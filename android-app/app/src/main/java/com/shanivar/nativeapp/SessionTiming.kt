package com.shanivar.nativeapp

/** Pure timing helpers keep passage pacing independent of Compose and wall-clock changes. */
fun passageIndex(remainingMillis: Long, totalMillis: Long, passageCount: Int): Int {
    require(totalMillis > 0)
    require(passageCount > 0)
    val elapsed = totalMillis - remainingMillis.coerceIn(0, totalMillis)
    return ((elapsed * passageCount) / totalMillis).toInt().coerceAtMost(passageCount - 1)
}
