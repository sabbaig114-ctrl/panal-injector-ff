package com.example

import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.formatBytes
import com.example.data.model.formatPkr
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun formatHelpers_returnExpectedStrings() {
        assertEquals("0 B", formatBytes(0L))
        assertEquals("512 B", formatBytes(512L))
        assertEquals("2.0 KB", formatBytes(2048L))
        assertEquals("1.50 MB", formatBytes(1_572_864L))
        assertEquals("PKR 1,500", formatPkr(1500L))
    }

    @Test
    fun enumParsing_handlesUnknownValuesSafely() {
        assertEquals(OrderStatus.APPROVED, OrderStatus.fromRaw("approved"))
        assertEquals(OrderStatus.PENDING, OrderStatus.fromRaw("UNKNOWN_STATUS"))
        assertEquals(PaymentMethod.JAZZCASH, PaymentMethod.fromRaw("jazzcash"))
        assertEquals(PaymentMethod.EASYPAISA, PaymentMethod.fromRaw(null))
    }
}
