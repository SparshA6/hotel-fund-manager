package com.sparsh.myapplication

import java.math.BigDecimal
import java.math.RoundingMode

data class PayoutBreakdown(
    val baseRate: Double,
    val propertyGst: Double,
    val grossAmount: Double,
    val commission: Double,
    val gstOnCommission: Double,
    val totalCommission: Double,
    val tds: Double,
    val tcs: Double,
    val processingFee: Double,
    val finalPayableAmount: Double
)

interface OtaCalculationStrategy {
    fun calculate(payableAmount: Double, settings: PortalSettings): PayoutBreakdown
}

fun roundValue(value: Double): Double {
    if (value.isNaN() || value.isInfinite()) return 0.0
    return BigDecimal(value.toString())
        .setScale(2, RoundingMode.HALF_UP)
        .toDouble()
}

class MmtGoibiboCalculationStrategy : OtaCalculationStrategy {
    override fun calculate(payableAmount: Double, settings: PortalSettings): PayoutBreakdown {
        val c = settings.commissionRate / 100.0
        val gProp = settings.propertyGstRate / 100.0
        val gComm = settings.gstOnCommissionRate / 100.0
        val tdsRate = settings.tdsRate / 100.0
        val tcsRate = settings.tcsRate / 100.0
        val s = settings.serviceCharge.toDouble()

        // Solve: payableAmount = [B * (1 + gProp) + s] * (1 - tds - tcs) - B * c * (1 + gComm)
        // payableAmount = B * [ (1 + gProp) * (1 - tds - tcs) - c * (1 + gComm) ] + s * (1 - tds - tcs)
        val denominator = (1.0 + gProp) * (1.0 - tdsRate - tcsRate) - c * (1.0 + gComm)
        val baseRate = if (denominator > 0.0) {
            roundValue((payableAmount - s * (1.0 - tdsRate - tcsRate)) / denominator)
        } else {
            payableAmount
        }

        val propertyGst = roundValue(baseRate * gProp)
        val propertyGrossCharges = roundValue(baseRate + propertyGst + s)
        
        val commission = roundValue(baseRate * c)
        val gstOnCommission = roundValue(commission * gComm)
        val totalCommission = roundValue(commission + gstOnCommission)
        
        val tds = roundValue(baseRate * tdsRate)
        val tcs = roundValue(baseRate * tcsRate)
        
        val payableToProperty = roundValue(propertyGrossCharges - totalCommission - tds - tcs)
        
        return PayoutBreakdown(
            baseRate = baseRate,
            propertyGst = propertyGst,
            grossAmount = propertyGrossCharges,
            commission = commission,
            gstOnCommission = gstOnCommission,
            totalCommission = totalCommission,
            tds = tds,
            tcs = tcs,
            processingFee = 0.0,
            finalPayableAmount = payableToProperty
        )
    }
}

class YatraCalculationStrategy : OtaCalculationStrategy {
    override fun calculate(payableAmount: Double, settings: PortalSettings): PayoutBreakdown {
        val c = settings.commissionRate / 100.0
        val tdsRate = settings.tdsRate / 100.0
        val tcsRate = settings.tcsRate / 100.0

        // Solve: payableAmount = B * (1 - c - tds - tcs)
        val denominator = 1.0 - c - tdsRate - tcsRate
        val baseRate = if (denominator > 0.0) {
            roundValue(payableAmount / denominator)
        } else {
            payableAmount
        }

        val referenceSellInclusive = baseRate
        val compensation = roundValue(baseRate * c)
        
        val tds = roundValue(referenceSellInclusive * tdsRate)
        val tcs = roundValue(referenceSellInclusive * tcsRate)
        
        val finalBaseRate = roundValue(referenceSellInclusive - compensation - tds - tcs)
        
        return PayoutBreakdown(
            baseRate = baseRate,
            propertyGst = 0.0,
            grossAmount = referenceSellInclusive,
            commission = compensation,
            gstOnCommission = 0.0,
            totalCommission = compensation,
            tds = tds,
            tcs = tcs,
            processingFee = 0.0,
            finalPayableAmount = finalBaseRate
        )
    }
}

class BookingComCalculationStrategy : OtaCalculationStrategy {
    override fun calculate(payableAmount: Double, settings: PortalSettings): PayoutBreakdown {
        val c = settings.commissionRate / 100.0
        val gProp = settings.propertyGstRate / 100.0
        val fee = settings.paymentProcessingFeeRate / 100.0

        // Solve: payableAmount = B * (1 + gProp) - B * c - B * (1 + gProp) * fee
        val denominator = (1.0 + gProp) * (1.0 - fee) - c
        val baseRate = if (denominator > 0.0) {
            roundValue(payableAmount / denominator)
        } else {
            payableAmount
        }

        val propertyGst = roundValue(baseRate * gProp)
        val reservationTotal = roundValue(baseRate + propertyGst)
        
        val commission = roundValue(baseRate * c)
        val processingFee = roundValue(reservationTotal * fee)
        
        val payableToProperty = roundValue(reservationTotal - commission - processingFee)
        
        return PayoutBreakdown(
            baseRate = baseRate,
            propertyGst = propertyGst,
            grossAmount = reservationTotal,
            commission = commission,
            gstOnCommission = 0.0,
            totalCommission = commission,
            tds = 0.0,
            tcs = 0.0,
            processingFee = processingFee,
            finalPayableAmount = payableToProperty
        )
    }
}

class DirectCalculationStrategy : OtaCalculationStrategy {
    override fun calculate(payableAmount: Double, settings: PortalSettings): PayoutBreakdown {
        return PayoutBreakdown(
            baseRate = payableAmount,
            propertyGst = 0.0,
            grossAmount = payableAmount,
            commission = 0.0,
            gstOnCommission = 0.0,
            totalCommission = 0.0,
            tds = 0.0,
            tcs = 0.0,
            processingFee = 0.0,
            finalPayableAmount = payableAmount
        )
    }
}

class DefaultCalculationStrategy : OtaCalculationStrategy {
    override fun calculate(payableAmount: Double, settings: PortalSettings): PayoutBreakdown {
        val c = settings.commissionRate / 100.0
        val gProp = settings.propertyGstRate / 100.0
        val gComm = settings.gstOnCommissionRate / 100.0
        val tdsRate = settings.tdsRate / 100.0
        val tcsRate = settings.tcsRate / 100.0

        // Solve: payableAmount = B * (1 + gProp) * (1 - tds - tcs) - B * c * (1 + gComm)
        val denominator = (1.0 + gProp) * (1.0 - tdsRate - tcsRate) - c * (1.0 + gComm)
        val baseRate = if (denominator > 0.0) {
            roundValue(payableAmount / denominator)
        } else {
            payableAmount
        }

        val propertyGst = roundValue(baseRate * gProp)
        val grossAmount = roundValue(baseRate + propertyGst)
        
        val commission = roundValue(baseRate * c)
        val gstOnCommission = roundValue(commission * gComm)
        val totalCommission = roundValue(commission + gstOnCommission)
        
        val tds = roundValue(baseRate * tdsRate)
        val tcs = roundValue(baseRate * tcsRate)
        
        val payable = roundValue(grossAmount - totalCommission - tds - tcs)
        
        return PayoutBreakdown(
            baseRate = baseRate,
            propertyGst = propertyGst,
            grossAmount = grossAmount,
            commission = commission,
            gstOnCommission = gstOnCommission,
            totalCommission = totalCommission,
            tds = tds,
            tcs = tcs,
            processingFee = 0.0,
            finalPayableAmount = payable
        )
    }
}

object OtaCalculationEngine {
    private val strategies = mapOf(
        "MMT" to MmtGoibiboCalculationStrategy(),
        "Goibibo" to MmtGoibiboCalculationStrategy(),
        "Yatra" to YatraCalculationStrategy(),
        "Booking.com" to BookingComCalculationStrategy(),
        "Direct" to DirectCalculationStrategy()
    )

    fun calculate(platform: String, payableAmount: Double, settings: PortalSettings): PayoutBreakdown {
        val strategy = strategies[platform] ?: DefaultCalculationStrategy()
        return strategy.calculate(payableAmount, settings)
    }
}
