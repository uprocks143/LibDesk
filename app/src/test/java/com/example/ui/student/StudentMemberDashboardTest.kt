package com.example.ui.student

import com.example.data.local.entities.StudentEntity
import com.example.ui.components.MembershipAlertLevel
import com.example.ui.components.calculateMembershipExpiration
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StudentMemberDashboardTest {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @Test
    fun testMembershipCalculation_ActiveFutureDate() {
        val futureDate = LocalDate.now().plusDays(25).format(formatter)
        val student = StudentEntity(
            id = "st-001",
            libraryId = "lib-001",
            studentCode = "LD-001",
            fullName = "Aarav Sharma",
            mobile = "9876543210",
            status = "ACTIVE",
            expiryDate = futureDate
        )

        val expiration = calculateMembershipExpiration(student)
        assertEquals(MembershipAlertLevel.ACTIVE, expiration.level)
        assertEquals(25, expiration.daysRemaining)
    }

    @Test
    fun testMembershipCalculation_ExpiringSoon() {
        val soonDate = LocalDate.now().plusDays(3).format(formatter)
        val student = StudentEntity(
            id = "st-002",
            libraryId = "lib-001",
            studentCode = "LD-002",
            fullName = "Priya Patel",
            mobile = "9876543211",
            status = "ACTIVE",
            expiryDate = soonDate
        )

        val expiration = calculateMembershipExpiration(student)
        assertEquals(MembershipAlertLevel.EXPIRING_CRITICAL, expiration.level)
        assertEquals(3, expiration.daysRemaining)
    }

    @Test
    fun testMembershipCalculation_ExpiredPastDate() {
        val pastDate = LocalDate.now().minusDays(5).format(formatter)
        val student = StudentEntity(
            id = "st-003",
            libraryId = "lib-001",
            studentCode = "LD-003",
            fullName = "Rohan Verma",
            mobile = "9876543212",
            status = "ACTIVE",
            expiryDate = pastDate
        )

        val expiration = calculateMembershipExpiration(student)
        assertEquals(MembershipAlertLevel.EXPIRED, expiration.level)
    }

    @Test
    fun testMembershipCalculation_BlankDate() {
        val student = StudentEntity(
            id = "st-004",
            libraryId = "lib-001",
            studentCode = "LD-004",
            fullName = "Neha Singh",
            mobile = "9876543213",
            status = "ACTIVE",
            expiryDate = ""
        )

        val expiration = calculateMembershipExpiration(student)
        assertEquals(MembershipAlertLevel.EXPIRED, expiration.level)
        assertEquals(0, expiration.daysRemaining)
    }
}
