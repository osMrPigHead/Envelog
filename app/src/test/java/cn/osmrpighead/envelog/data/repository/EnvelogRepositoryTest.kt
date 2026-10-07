package cn.osmrpighead.envelog.data.repository

import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.model.ExpenditureEnvelopeAllocation
import cn.osmrpighead.envelog.data.model.Transaction
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

abstract class EnvelogRepositoryTest {
    protected val startDate = LocalDate.of(2026, 9, 1)!!
    protected val endDate = LocalDate.of(2026, 10, 1)!!
    protected val clearStartDate = LocalDate.of(2026, 9, 16)!!
    protected val clearEndDate = LocalDate.of(2026, 9, 20)!!
    protected val transactionTime = ZonedDateTime.of(2026, 9, 28, 8, 0, 0, 0, ZoneId.systemDefault())!!

    private lateinit var envelopes: Set<Envelope>
    private lateinit var envelopeWithBudgets: Envelope
    private lateinit var envelopeWithAllocations: Envelope
    private lateinit var envelopeWithoutAllocations: Envelope
    private lateinit var budgets: Set<Budget>
    private lateinit var transactions: Set<Transaction>
    private lateinit var allocations: Set<ExpenditureEnvelopeAllocation>
    private lateinit var repository: EnvelogRepository
    protected abstract fun getActiveEnvelopes(): Set<Envelope>
    protected abstract fun getEnvelopeWithBudgets(): Envelope
    protected abstract fun getEnvelopeWithAndWithoutAllocations(): Pair<Envelope, Envelope>
    protected abstract fun getBudgetsDuringTest(): Set<Budget>
    protected abstract fun getTransactionsDuringTest(): Set<Transaction>
    protected abstract fun getAllocationsDuringTest(): Set<ExpenditureEnvelopeAllocation>
    protected abstract fun createRepository(): EnvelogRepository

    @Before
    fun setUp() {
        envelopes = getActiveEnvelopes()
        envelopeWithBudgets = getEnvelopeWithBudgets()
        getEnvelopeWithAndWithoutAllocations().let {
            envelopeWithAllocations = it.first
            envelopeWithoutAllocations = it.second
        }
        budgets = getBudgetsDuringTest()
        transactions = getTransactionsDuringTest()
        allocations = getAllocationsDuringTest()
        repository = createRepository()
    }

    @Test
    fun observeActiveEnvelopes_ArchivedNotListedAndActiveListed() {
        runBlocking {
            repository.observeActiveEnvelopes().take(1).collect {
                assertEquals(envelopes, it.toSet())
            }
        }
    }

    @Test
    fun observeBudgetsBetween_JudgedByStartDate() {
        runBlocking {
            repository.observeBudgets(startDate, endDate).take(1).collect {
                assertEquals(budgets, it.toSet())
            }
        }
    }

    @Test
    fun observeExpenditureBetween_JudgedByStartDate() {
        runBlocking {
            repository.observeExpenditureEnvelopeAllocations(startDate, endDate).take(1).collect {
                assertEquals(allocations, it.toSet())
            }
        }
    }

    @Test
    fun createEnvelope_CreatedBeListed() {
        runBlocking {
            val name1 = "Created1"
            val name2 = "Created2"
            val id1 = repository.createEnvelope(name1, true).getOrNull()
            val id2 = repository.createEnvelope(name2, false).getOrNull()
            assertNotNull(id1)
            assertNotNull(id2)
            repository.observeActiveEnvelopes().take(1).collect {
                assertEquals(name1, it.find { envelope -> envelope.id == id1 }?.name)
                assertEquals(name2, it.find { envelope -> envelope.id == id2 }?.name)
            }
        }
    }

    @Test
    fun renameEnvelope_RenamedBeQueried() {
        runBlocking {
            val id = envelopes.elementAt(0).id
            val newName = "Renamed"
            repository.renameEnvelope(id, newName)
            repository.observeActiveEnvelopes().take(1).collect {
                assertEquals(id, it.find { envelope -> envelope.name == newName }?.id)
            }
        }
    }

    @Test
    fun archiveEnvelope_ArchivedNotListed() {
        runBlocking {
            val id = envelopes.elementAt(0).id
            repository.archiveEnvelope(id)
            repository.observeActiveEnvelopes().take(1).collect {
                assert(!it.any { envelope -> envelope.id == id })
            }
        }
    }

    @Test
    fun deleteEnvelope_ReferencedDownUnreferencedUp() {
        runBlocking {
            assert(repository.deleteEnvelope(envelopeWithAllocations.id).isFailure)
            assert(repository.deleteEnvelope(envelopeWithoutAllocations.id).isSuccess)
            repository.observeActiveEnvelopes().take(1).collect {
                assert(it.any { envelope -> envelope.id == envelopeWithAllocations.id })
                assert(!it.any { envelope -> envelope.id == envelopeWithoutAllocations.id })
            }
        }
    }

    @Test
    fun clearBudget_ById_ClearedNotListed() {
        runBlocking {
            val id = budgets.elementAt(0).id
            repository.clearBudget(id)
            repository.observeBudgets(startDate, endDate).take(1).collect {
                assert(!it.any { budget -> budget.id == id })
            }
        }
    }

    @Test
    fun clearBudget_ByEnvelopeAndDate_ClearedNotListed() {
        runBlocking {
            val id = envelopeWithBudgets.id
            repository.clearBudget(id, clearStartDate, clearEndDate)
            repository.observeBudgets(startDate, endDate).take(1).collect { budgets ->
                assert(
                    !budgets.any {
                        it.envelopeId == id && maxOf(clearStartDate, it.startDate) < minOf(clearEndDate, it.endDate)
                    },
                )
            }
        }
    }

    @Test
    fun clearBudget_ByDate_ClearedNotListed() {
        runBlocking {
            repository.clearBudget(clearStartDate, clearEndDate)
            repository.observeBudgets(startDate, endDate).take(1).collect { budgets ->
                assert(
                    !budgets.any { maxOf(clearStartDate, it.startDate) < minOf(clearEndDate, it.endDate) },
                )
            }
        }
    }

    @Test
    fun setBudget_ById_ChangedBeQueried() {
        runBlocking {
            val id = budgets.elementAt(0).id
            val newAmount = 800.toLong()
            repository.setBudget(id, newAmount)
            repository.observeBudgets(startDate, endDate).take(1).collect {
                assertEquals(newAmount, it.find { budget -> budget.id == id }?.amount)
            }
        }
    }

    @Test
    fun setBudget_NotClearedByDate_Failure() {
        runBlocking {
            assert(repository.setBudget(envelopes.elementAt(0).id, startDate, endDate, 2000).isFailure)
        }
    }

    @Test
    fun setBudget_ClearedByDate_BeQueried() {
        runBlocking {
            val id = envelopes.elementAt(0).id
            repository.clearBudget(id, startDate, endDate)
            repository.setBudget(id, startDate, endDate, 2000)
        }
    }

    @Test
    fun recordExpenditure_MatchedAllocation_BeQueried() {
        runBlocking {
            val envelopeId = envelopes.elementAt(0).id
            val amount = 400.toLong()
            val transactionId = repository.recordExpenditure(
                dateTime = transactionTime,
                amount = amount,
                allocations = mapOf(envelopeId to amount),
            ).getOrNull()
            assertNotNull(transactionId)
            repository.observeExpenditureEnvelopeAllocations(startDate, endDate).take(1).collect {
                assert(
                    it.contains(
                        ExpenditureEnvelopeAllocation(
                            transactionId = transactionId!!,
                            envelopeId = envelopeId,
                            amount = amount,
                        ),
                    ),
                )
            }
        }
    }

    @Test
    fun recordExpenditure_MismatchedAllocation_Failure() {
        runBlocking {
            val envelopeId = envelopes.elementAt(0).id
            assert(
                repository.recordExpenditure(
                    dateTime = transactionTime,
                    amount = 400,
                    allocations = mapOf(envelopeId to 200),
                ).isFailure,
            )
        }
    }

    @Test
    fun reallocate_MatchedAllocation_BeQueried() {
        runBlocking {
            val transaction = transactions.elementAt(0)
            var envelopeId = envelopes.random().id
            assert(repository.reallocate(transaction.id, mapOf(envelopeId to transaction.amount)).isSuccess)
            repository.observeExpenditureEnvelopeAllocations(startDate, endDate).take(1).collect {
                assert(
                    it.contains(
                        ExpenditureEnvelopeAllocation(
                            transactionId = transaction.id,
                            envelopeId = envelopeId,
                            amount = transaction.amount,
                        ),
                    ),
                )
            }
            val newAmount = 800.toLong()
            envelopeId = envelopes.random().id
            assert(repository.reallocate(transaction.id, newAmount, mapOf(envelopeId to newAmount)).isSuccess)
            repository.observeExpenditureEnvelopeAllocations(startDate, endDate).take(1).collect {
                assert(
                    it.contains(
                        ExpenditureEnvelopeAllocation(
                            transactionId = transaction.id,
                            envelopeId = envelopeId,
                            amount = newAmount,
                        ),
                    ),
                )
            }
        }
    }

    @Test
    fun reallocate_MismatchedAllocation_Failure() {
        runBlocking {
            val transaction = transactions.elementAt(0)
            val envelopeId = envelopes.random().id
            assert(repository.reallocate(transaction.id, mapOf(envelopeId to transaction.amount + 1)).isFailure)
            assert(repository.reallocate(transaction.id, 400, mapOf(envelopeId to 200)).isFailure)
        }
    }

    @Test
    fun deleteTransaction_DeletedNotListed() {
        runBlocking {
            val id = transactions.elementAt(0).id
            repository.deleteTransaction(id)
            repository.observeExpenditureEnvelopeAllocations(startDate, endDate).take(1).collect {
                assert(!it.any { allocation -> allocation.transactionId == id })
            }
        }
    }
}