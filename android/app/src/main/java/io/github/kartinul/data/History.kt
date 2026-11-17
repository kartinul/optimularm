package io.github.kartinul.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

private const val KEEP = 60
private const val TRIAL = 3 // nights a feelGoodAfter value gets before its neighbours are tried
private const val TUNE_STEP = 5
private val TUNE_RANGE = 0..30

/** A rated morning. [optimized]: the alarm was moved to a cycle end, so the rating says something about feelGoodAfter. */
data class Night(
    val sleep: Long?,
    val ring: Long,
    val rating: Int,
    val feelGoodAfter: Int,
    val optimized: Boolean,
)

// shortcut: kept on the phone only, so a reinstall starts history and tuning over; move to Firestore if that matters
private fun prefs(context: Context) = context.getSharedPreferences("history", Context.MODE_PRIVATE)

fun loadNights(context: Context): List<Night> {
    val array = JSONArray(prefs(context).getString("nights", "[]"))
    return (0 until array.length()).map { array.getJSONObject(it) }.map {
        Night(
            sleep = it.optLong("sleep").takeIf { sleep -> sleep > 0 },
            ring = it.getLong("ring"),
            rating = it.getInt("rating"),
            feelGoodAfter = it.getInt("feelGoodAfter"),
            optimized = it.getBoolean("optimized"),
        )
    }
}

fun recordNight(context: Context, night: Night) {
    val nights = (loadNights(context) + night).takeLast(KEEP)
    val array = JSONArray(nights.map {
        JSONObject().put("sleep", it.sleep ?: 0).put("ring", it.ring).put("rating", it.rating)
            .put("feelGoodAfter", it.feelGoodAfter).put("optimized", it.optimized)
    })
    prefs(context).edit { putString("nights", array.toString()) }
}

/** Hill-climbs on ratings: give a value [TRIAL] nights, try its untried neighbours, then settle on the best average. */
fun tunedFeelGoodAfter(nights: List<Night>, current: Int): Int {
    val rated = nights.filter { it.optimized }.groupBy { it.feelGoodAfter }
    if ((rated[current]?.size ?: 0) < TRIAL) return current
    val untried = listOf(current - TUNE_STEP, current + TUNE_STEP).filter { it in TUNE_RANGE && it !in rated }
    return untried.firstOrNull() ?: rated.maxBy { (_, group) -> group.map { it.rating }.average() }.key
}
