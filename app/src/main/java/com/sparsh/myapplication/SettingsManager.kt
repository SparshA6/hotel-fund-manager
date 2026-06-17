package com.sparsh.myapplication

import android.content.Context
import android.content.SharedPreferences

data class DeductionBreakdown(
    val guestAmount: Double,
    val tax: Double,
    val base: Double,
    val commission: Double,
    val tds: Double,
    val tcs: Double,
    val totalDeductions: Double,
    val netPayout: Double
)

object SettingsManager {
    private const val PREFS_NAME = "commission_settings"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getTaxRate(context: Context): Float = getPrefs(context).getFloat("tax_rate", 5.0f)
    fun setTaxRate(context: Context, value: Float) = getPrefs(context).edit().putFloat("tax_rate", value).apply()

    fun getTdsRate(context: Context): Float = getPrefs(context).getFloat("tds_rate", 0.5f)
    fun setTdsRate(context: Context, value: Float) = getPrefs(context).edit().putFloat("tds_rate", value).apply()

    fun getTcsRate(context: Context): Float = getPrefs(context).getFloat("tcs_rate", 0.1f)
    fun setTcsRate(context: Context, value: Float) = getPrefs(context).edit().putFloat("tcs_rate", value).apply()

    fun getCommissionRate(context: Context, platform: String): Float {
        val key = "comm_$platform"
        val defaultVal = when (platform) {
            "MMT" -> 20.0f
            "Booking.com" -> 15.0f
            "Agoda" -> 15.0f
            "Goibibo" -> 15.0f
            "Cleartrip" -> 12.0f
            else -> 0.0f
        }
        return getPrefs(context).getFloat(key, defaultVal)
    }

    fun setCommissionRate(context: Context, platform: String, value: Float) {
        val key = "comm_$platform"
        getPrefs(context).edit().putFloat(key, value).apply()
    }

    fun calculateBreakdown(context: Context, platform: String, guestAmount: Double): DeductionBreakdown {
        if (platform.equals("Direct", ignoreCase = true)) {
            return DeductionBreakdown(
                guestAmount = guestAmount,
                tax = 0.0,
                base = guestAmount,
                commission = 0.0,
                tds = 0.0,
                tcs = 0.0,
                totalDeductions = 0.0,
                netPayout = guestAmount
            )
        }

        val taxPct = getTaxRate(context) / 100.0
        val commPct = getCommissionRate(context, platform) / 100.0
        val tdsPct = getTdsRate(context) / 100.0
        val tcsPct = getTcsRate(context) / 100.0

        val base = guestAmount / (1.0 + taxPct)
        val tax = base * taxPct
        val commission = base * commPct
        val tds = base * tdsPct
        val tcs = base * tcsPct
        val totalDeductions = commission + tds + tcs
        val netPayout = guestAmount - totalDeductions

        return DeductionBreakdown(
            guestAmount = guestAmount,
            tax = tax,
            base = base,
            commission = commission,
            tds = tds,
            tcs = tcs,
            totalDeductions = totalDeductions,
            netPayout = netPayout
        )
    }
}
