package com.sparsh.myapplication

data class PortalSettings(
    val platform: String,
    val commissionRate: Float = 0f,
    val propertyGstRate: Float = 0f,
    val gstOnCommissionRate: Float = 0f,
    val tdsRate: Float = 0f,
    val tcsRate: Float = 0f,
    val paymentProcessingFeeRate: Float = 0f,
    val serviceCharge: Float = 0f
)
