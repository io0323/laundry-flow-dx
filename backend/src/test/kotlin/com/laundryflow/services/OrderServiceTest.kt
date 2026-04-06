package com.laundryflow.services

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class OrderServiceTest : StringSpec({
    val service = OrderService()

    "Calculate price for base item (シャツ - 300)" {
        service.calculateItemPrice("シャツ", 1, false, false) shouldBe 300
    }

    "Calculate price for multiple units (シャツ×3 = 900)" {
        service.calculateItemPrice("シャツ", 3, false, false) shouldBe 900
    }

    "Calculate price with stain removal (スーツ 1500 + 500 = 2000)" {
        service.calculateItemPrice("スーツ", 1, true, false) shouldBe 2000
    }

    "Calculate price with stain removal for multiple units ((シャツ 300 + 500) * 2 = 1600)" {
        service.calculateItemPrice("シャツ", 2, true, false) shouldBe 1600
    }

    "Calculate price with rush order (+30% truncated, ドレス 1800 * 1.3 = 2340)" {
        service.calculateItemPrice("ドレス", 1, false, true) shouldBe 2340
    }

    "Calculate price with rush order for multiple units ((コート 2000 * 2) * 1.3 = 5200)" {
        service.calculateItemPrice("コート", 2, false, true) shouldBe 5200
    }

    "Calculate price with BOTH stain and rush ((毛布 2500 + 500) * 1 * 1.3 = 3900)" {
        // Business logic strictly dictates adding +500 for stain BEFORE 1.3 rush multiplier
        service.calculateItemPrice("毛布", 1, true, true) shouldBe 3900
    }

    "Calculate price with BOTH stain and rush and multiple units ((シャツ 300 + 500) * 3 * 1.3 = 3120)" {
        service.calculateItemPrice("シャツ", 3, true, true) shouldBe 3120
    }

    "Verify default target date (Regular) is exactly 3 days after receive date" {
        val received = LocalDate.of(2026, 4, 1)
        val target = service.calculateDefaultTargetDate(received, false)
        target shouldBe LocalDate.of(2026, 4, 4)
    }

    "Verify default target date (Rush) is exactly 1 day after receive date" {
        val received = LocalDate.of(2026, 4, 1)
        val target = service.calculateDefaultTargetDate(received, true)
        target shouldBe LocalDate.of(2026, 4, 2)
    }
})
