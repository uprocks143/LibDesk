package com.example.domain.model

data class DashboardKpiState(
    val totalSeats: Int = 0,
    val occupiedSeats: Int = 0,
    val availableSeats: Int = 0,
    val reservedSeats: Int = 0,
    val maintenanceSeats: Int = 0,
    val occupancyPercent: Int = 0,
    val activeStudentsCount: Int = 0,
    val expiringCount: Int = 0,
    val totalStudentsCount: Int = 0,
    val todayCollection: Double = 0.0,
    val pendingDuesTotal: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netRevenue: Double = 0.0,
    val issuedBooksCount: Int = 0,
    val totalBooksCount: Int = 0,
    val liveInHallCount: Int = 0
)

object KpiCalculationLogic {
    fun calculate(
        seats: List<com.example.data.local.entities.SeatEntity>,
        students: List<com.example.data.local.entities.StudentEntity>,
        payments: List<com.example.data.local.entities.PaymentEntity>,
        expenses: List<com.example.data.local.entities.ExpenseEntity>,
        attendance: List<com.example.data.local.entities.AttendanceEntity>,
        bookIssues: List<com.example.data.local.entities.BookIssueEntity>,
        books: List<com.example.data.local.entities.PhysicalBookEntity>,
        todayDateStr: String
    ): DashboardKpiState {
        val totalSeats = seats.size
        val occupiedSeats = seats.count { it.status.equals("OCCUPIED", ignoreCase = true) }
        val availableSeats = seats.count { it.status.equals("AVAILABLE", ignoreCase = true) }
        val reservedSeats = seats.count { it.status.equals("RESERVED", ignoreCase = true) }
        val maintenanceSeats = seats.count { it.status.equals("MAINTENANCE", ignoreCase = true) }
        val occupancyPercent = if (totalSeats > 0) (occupiedSeats * 100) / totalSeats else 0

        val activeStudentsCount = students.count { it.status.equals("ACTIVE", ignoreCase = true) }
        val expiringCount = students.count { it.status.equals("EXPIRED", ignoreCase = true) }
        val pendingDuesTotal = students.sumOf { it.dueAmount }

        val oneDayMillis = 86400000L
        val now = System.currentTimeMillis()
        val todayCollection = payments.filter { 
            it.date == todayDateStr || (now - it.createdAt < oneDayMillis) 
        }.sumOf { it.amount }

        val totalIncome = payments.sumOf { it.amount }
        val totalExpense = expenses.sumOf { it.amount }
        val netRevenue = totalIncome - totalExpense

        val issuedBooksCount = bookIssues.count { it.status.equals("ISSUED", ignoreCase = true) }
        val liveInHallCount = attendance.count { it.status.equals("CHECKED_IN", ignoreCase = true) }

        return DashboardKpiState(
            totalSeats = totalSeats,
            occupiedSeats = occupiedSeats,
            availableSeats = availableSeats,
            reservedSeats = reservedSeats,
            maintenanceSeats = maintenanceSeats,
            occupancyPercent = occupancyPercent,
            activeStudentsCount = activeStudentsCount,
            expiringCount = expiringCount,
            totalStudentsCount = students.size,
            todayCollection = todayCollection,
            pendingDuesTotal = pendingDuesTotal,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netRevenue = netRevenue,
            issuedBooksCount = issuedBooksCount,
            totalBooksCount = books.size,
            liveInHallCount = liveInHallCount
        )
    }
}
