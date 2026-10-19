package io.github.kartinul.data

import android.content.Context
import androidx.core.content.edit

/** The cycle math's tunables, in minutes. Phone-only, like [AlarmPrefs]. */
data class CalcPrefs(
    val fallAsleep: Int = 15,
    val cycle: Int = 90,
    val feelGood: Int = 15,
    val lateNap: Int = 30,
)

private fun prefs(context: Context) = context.getSharedPreferences("calc", Context.MODE_PRIVATE)

fun loadCalcPrefs(context: Context): CalcPrefs = prefs(context).let {
    val defaults = CalcPrefs()
    CalcPrefs(
        fallAsleep = it.getInt("fallAsleep", defaults.fallAsleep),
        cycle = it.getInt("cycle", defaults.cycle),
        feelGood = it.getInt("feelGood", defaults.feelGood),
        lateNap = it.getInt("lateNap", defaults.lateNap),
    )
}

fun saveCalcPrefs(context: Context, calc: CalcPrefs) = prefs(context).edit {
    putInt("fallAsleep", calc.fallAsleep)
    putInt("cycle", calc.cycle)
    putInt("feelGood", calc.feelGood)
    putInt("lateNap", calc.lateNap)
}
