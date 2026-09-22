package com.example.ui.student

import com.example.data.local.entities.HallEntity
import com.example.data.local.entities.SeatEntity
import com.example.data.local.entities.StudentEntity
import org.junit.Assert.*
import org.junit.Test

class StudentSeatLayoutTest {

    @Test
    fun testSeatOccupancyCalculations() {
        val seats = listOf(
            SeatEntity(id = "s1", libraryId = "lib-1", seatNumber = "A-01", hallName = "Main Hall", status = "AVAILABLE"),
            SeatEntity(id = "s2", libraryId = "lib-1", seatNumber = "A-02", hallName = "Main Hall", status = "AVAILABLE"),
            SeatEntity(id = "s3", libraryId = "lib-1", seatNumber = "A-03", hallName = "Main Hall", status = "OCCUPIED"),
            SeatEntity(id = "s4", libraryId = "lib-1", seatNumber = "A-04", hallName = "Main Hall", status = "RESERVED"),
            SeatEntity(id = "s5", libraryId = "lib-1", seatNumber = "A-05", hallName = "Main Hall", status = "MAINTENANCE")
        )

        val total = seats.size
        val available = seats.count { it.status.equals("AVAILABLE", ignoreCase = true) }
        val occupied = seats.count { it.status.equals("OCCUPIED", ignoreCase = true) }
        val reserved = seats.count { it.status.equals("RESERVED", ignoreCase = true) }

        assertEquals(5, total)
        assertEquals(2, available)
        assertEquals(1, occupied)
        assertEquals(1, reserved)

        val occupancyPercent = (((occupied + reserved).toFloat() / total) * 100).toInt()
        assertEquals(40, occupancyPercent)
    }

    @Test
    fun testStudentAssignedSeatMatching() {
        val student = StudentEntity(
            id = "st-001",
            libraryId = "lib-001",
            studentCode = "LD-001",
            fullName = "Rahul Sharma",
            mobile = "9876543210",
            seatNumber = "B-05",
            seatId = "seat-b05",
            hallName = "AC Silent Pods"
        )

        val seat1 = SeatEntity(id = "seat-b05", libraryId = "lib-001", seatNumber = "B-05", hallName = "AC Silent Pods", status = "OCCUPIED")
        val seat2 = SeatEntity(id = "seat-b06", libraryId = "lib-001", seatNumber = "B-06", hallName = "AC Silent Pods", status = "AVAILABLE")

        val isMySeat1 = (student.seatNumber.isNotBlank() && seat1.seatNumber.equals(student.seatNumber, ignoreCase = true)) ||
                (student.seatId.isNotBlank() && seat1.id == student.seatId)

        val isMySeat2 = (student.seatNumber.isNotBlank() && seat2.seatNumber.equals(student.seatNumber, ignoreCase = true)) ||
                (student.seatId.isNotBlank() && seat2.id == student.seatId)

        assertTrue(isMySeat1)
        assertFalse(isMySeat2)
    }

    @Test
    fun testHallAndSearchFiltering() {
        val seats = listOf(
            SeatEntity(id = "s1", libraryId = "lib-1", seatNumber = "A-01", hallId = "h1", hallName = "Quiet Hall", seatType = "Window Pod", status = "AVAILABLE"),
            SeatEntity(id = "s2", libraryId = "lib-1", seatNumber = "A-02", hallId = "h1", hallName = "Quiet Hall", seatType = "Standard Desk", status = "OCCUPIED"),
            SeatEntity(id = "s3", libraryId = "lib-1", seatNumber = "B-01", hallId = "h2", hallName = "Reading Hall", seatType = "Window Pod", status = "AVAILABLE")
        )

        // Filter by Hall h1
        val h1Seats = seats.filter { it.hallId == "h1" }
        assertEquals(2, h1Seats.size)

        // Filter by Status AVAILABLE
        val availableSeats = seats.filter { it.status.equals("AVAILABLE", true) }
        assertEquals(2, availableSeats.size)

        // Filter by Search Query "Window"
        val query = "Window"
        val windowDesks = seats.filter {
            it.seatNumber.contains(query, true) || it.seatType.contains(query, true)
        }
        assertEquals(2, windowDesks.size)
    }
}
