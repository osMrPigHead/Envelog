package cn.osmrpighead.envelog.data.repository

import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.model.ExpenditureEnvelopeAllocation
import cn.osmrpighead.envelog.data.model.Transaction
import cn.osmrpighead.envelog.data.model.TransactionType
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

class InMemoryEnvelogRepositoryTest : EnvelogRepositoryTest() {
    private val envelopesOffTest = listOf(
        Envelope(
            id = UUID.randomUUID(),
            name = "Envelope -B+A",
            isBudgeted = false,
            archived = true,
        ),
        Envelope(
            id = UUID.randomUUID(),
            name = "Envelope +B+A",
            isBudgeted = true,
            archived = true,
        ),
    )
    private val activeEnvelopes = listOf(
        Envelope(
            id = UUID.randomUUID(),
            name = "Envelope -B-A",
            isBudgeted = false,
            archived = false,
        ),
        Envelope(
            id = UUID.randomUUID(),
            name = "Envelope +B-A 1",
            isBudgeted = true,
            archived = false,
        ),
        Envelope(
            id = UUID.randomUUID(),
            name = "Envelope +B-A 7",
            isBudgeted = true,
            archived = false,
        ),
        Envelope(
            id = UUID.randomUUID(),
            name = "Envelope +B-A 14",
            isBudgeted = true,
            archived = false,
        ),
        Envelope(
            id = UUID.randomUUID(),
            name = "Envelope +B-A 28",
            isBudgeted = true,
            archived = false,
        ),
        Envelope(
            id = UUID.randomUUID(),
            name = "Envelope +B-A Empty",
            isBudgeted = true,
            archived = false,
        ),
    )
    private val envelopeWithBudgets = activeEnvelopes[1]
    private val envelopeWithAndWithoutAllocation = Pair(activeEnvelopes[1], activeEnvelopes[5])
    private val budgetsOffTest = listOf(
        Budget(
            id = UUID.randomUUID(),
            envelopeId = activeEnvelopes[2].id,
            startDate = startDate.minusDays(3),
            endDate = startDate,
            amount = 2000,
        ),
        Budget(
            id = UUID.randomUUID(),
            envelopeId = activeEnvelopes[2].id,
            startDate = startDate.minusDays(1),
            endDate = endDate,
            amount = 2000,
        ),
        Budget(
            id = UUID.randomUUID(),
            envelopeId = activeEnvelopes[2].id,
            startDate = endDate,
            endDate = endDate.plusDays(1),
            amount = 2000,
        ),
    ).toMutableList().apply {
        addAll(
            envelopesOffTest.toMutableList().apply { add(activeEnvelopes[0]) }.map {
                Budget(
                    id = UUID.randomUUID(),
                    envelopeId = it.id,
                    startDate = startDate,
                    endDate = endDate,
                    amount = 2000,
                )
            },
        )
    }
    private val budgetsDuringTest = emptyList<Budget>().toMutableList().apply {
        fun addBudgets(envelopeIdx: Int, size: Int, period: Long) {
            addAll(
                List(size) {
                    Budget(
                        id = UUID.randomUUID(),
                        envelopeId = activeEnvelopes[envelopeIdx].id,
                        startDate = startDate.plusDays(it.toLong() * period),
                        endDate = startDate.plusDays(it.toLong() * period + period),
                        amount = 2000,
                    )
                },
            )
        }
        addBudgets(1, 30, 1)
        addBudgets(2, 5, 7)
        addBudgets(3, 3, 14)
        addBudgets(4, 2, 28)
    }
    private val transactionsOffTest = listOf(
        Transaction(
            id = UUID.randomUUID(),
            type = TransactionType.Expenditure,
            dateTime = ZonedDateTime.of(startDate.minusDays(1), LocalTime.of(8, 0), ZoneId.systemDefault()),
            amount = 2000,
        ),
        Transaction(
            id = UUID.randomUUID(),
            type = TransactionType.Expenditure,
            dateTime = ZonedDateTime.of(endDate, LocalTime.of(8, 0), ZoneId.systemDefault()),
            amount = 2000,
        ),
    )
    private val transactionsDuringTest = List(30) {
        Transaction(
            id = UUID.randomUUID(),
            type = TransactionType.Expenditure,
            dateTime = ZonedDateTime.of(startDate.plusDays(it.toLong()), LocalTime.of(8, 0), ZoneId.systemDefault()),
            amount = 2000,
        )
    }
    private val allocationsOffTest = transactionsOffTest.map {
        ExpenditureEnvelopeAllocation(
            transactionId = it.id,
            envelopeId = activeEnvelopes[1].id,
            amount = 2000,
        )
    }
    private val allocationsDuringTest = transactionsDuringTest.map {
        ExpenditureEnvelopeAllocation(
            transactionId = it.id,
            envelopeId = activeEnvelopes[1].id,
            amount = 2000,
        )
    }

    override fun getActiveEnvelopes(): Set<Envelope> = activeEnvelopes.toSet()
    override fun getEnvelopeWithBudgets(): Envelope = envelopeWithBudgets
    override fun getEnvelopeWithAndWithoutAllocations(): Pair<Envelope, Envelope> = envelopeWithAndWithoutAllocation
    override fun getBudgetsDuringTest(): Set<Budget> = budgetsDuringTest.toSet()
    override fun getTransactionsDuringTest(): Set<Transaction> = transactionsDuringTest.toSet()
    override fun getAllocationsDuringTest(): Set<ExpenditureEnvelopeAllocation> = allocationsDuringTest.toSet()

    override fun createRepository(): EnvelogRepository = InMemoryEnvelogRepository(
        envelopes = envelopesOffTest.toMutableList().apply { addAll(activeEnvelopes) },
        budgets = budgetsOffTest.toMutableList().apply { addAll(budgetsDuringTest) },
        transactions = transactionsOffTest.toMutableList().apply { addAll(transactionsDuringTest) },
        allocations = allocationsOffTest.toMutableList().apply { addAll(allocationsDuringTest) },
    )
}