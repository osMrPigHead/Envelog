package cn.osmrpighead.envelog.data.repository

import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.model.ExpenditureEnvelopeAllocation
import cn.osmrpighead.envelog.data.model.Transaction
import cn.osmrpighead.envelog.data.model.TransactionType
import cn.osmrpighead.envelog.data.query.DateRange
import cn.osmrpighead.envelog.data.query.DateRangeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

internal object TestData {
    val periodStart: LocalDate = LocalDate.of(2026, 10, 1)
    val periodEnd: LocalDate = LocalDate.of(2026, 11, 1)

    val period: DateRange = range()
    val overlapPeriod: DateRange = range(rule = DateRangeRule.Overlap)

    val envelopeAId: UUID = UUID.fromString("00000000-0000-0000-0000-00000000000a")
    val envelopeBId: UUID = UUID.fromString("00000000-0000-0000-0000-00000000000b")
    val archivedEnvelopeId: UUID = UUID.fromString("00000000-0000-0000-0000-00000000000c")

    val budgetAId: UUID = UUID.fromString("00000000-0000-0000-0000-00000000000d")
    val budgetBId: UUID = UUID.fromString("00000000-0000-0000-0000-00000000000e")
    val budgetCId: UUID = UUID.fromString("00000000-0000-0000-0000-00000000000f")
    val budgetDId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000010")

    val transactionAId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000011")
    val transactionBId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000012")

    val unknownId: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000ff")

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    fun range(
        start: LocalDate = periodStart,
        end: LocalDate = periodEnd,
        rule: DateRangeRule = DateRangeRule.StartInRange,
    ): DateRange = DateRange(start = start, end = end, rule = rule)

    fun at(date: LocalDate): ZonedDateTime = ZonedDateTime.of(date, LocalTime.of(12, 0), zone)

    fun envelope(
        id: UUID = TestData.envelopeAId,
        name: String = "Food",
        isBudgeted: Boolean = true,
        archived: Boolean = false,
    ): Envelope = Envelope(id = id, name = name, isBudgeted = isBudgeted, archived = archived)

    fun budget(
        id: UUID = TestData.budgetAId,
        envelopeId: UUID = TestData.envelopeAId,
        startDate: LocalDate = periodStart,
        endDate: LocalDate = periodEnd,
        amount: Long = 100_000,
    ): Budget = Budget(
        id = id,
        envelopeId = envelopeId,
        startDate = startDate,
        endDate = endDate,
        amount = amount,
    )

    fun transaction(
        id: UUID = TestData.transactionAId,
        type: TransactionType = TransactionType.Expenditure,
        date: LocalDate = periodStart.plusDays(4),
        amount: Long = 10_000,
    ): Transaction = Transaction(id = id, type = type, dateTime = at(date), amount = amount)

    fun allocation(
        transactionId: UUID = TestData.transactionAId,
        envelopeId: UUID = TestData.envelopeAId,
        amount: Long = 10_000,
    ): ExpenditureEnvelopeAllocation = ExpenditureEnvelopeAllocation(
        transactionId = transactionId,
        envelopeId = envelopeId,
        amount = amount,
    )

    fun repository(
        envelopes: List<Envelope> = emptyList(),
        budgets: List<Budget> = emptyList(),
        transactions: List<Transaction> = emptyList(),
        allocations: List<ExpenditureEnvelopeAllocation> = emptyList(),
    ): InMemoryEnvelogRepository = InMemoryEnvelogRepository(
        envelopes = envelopes,
        budgets = budgets,
        transactions = transactions,
        allocations = allocations,
    )
}

internal fun <T> Result<T>.assertFailureIs(expected: Class<out Throwable>) {
    assertTrue("Expected a failure, but got ${getOrNull()}", isFailure)
    assertEquals(expected, exceptionOrNull()!!::class.java)
}
