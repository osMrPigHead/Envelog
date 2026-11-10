package cn.osmrpighead.envelog.data.repository

import android.app.Application
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import cn.osmrpighead.envelog.testing.RoomTestData
import cn.osmrpighead.envelog.testing.seed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class DemoDatabaseSeederTest {
    private lateinit var database: EnvelogDatabase
    private lateinit var seeder: DemoDatabaseSeeder

    @Before
    fun setUp() {
        database = RoomTestData.createDatabase()
        seeder = DemoDatabaseSeeder(
            database = database,
            clock = Clock.fixed(RoomTestData.at(RoomTestData.today), RoomTestData.zone),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun totalAllocated(): Long = database.envelopeDao()
        .observeSummaries(
            today = RoomTestData.today,
            yearMonth = RoomTestData.month,
            spanStart = RoomTestData.monthStart,
            spanEnd = RoomTestData.monthEnd,
        )
        .first()
        .sumOf { it.allocated }

    @Test
    fun seedIfNeeded_insertsDemoDataOnlyOnceOnEmptyDatabase() = runTest {
        seeder.seedIfNeeded()
        seeder.seedIfNeeded()

        assertEquals(5, database.seedDao().envelopeCount())
        assertEquals(104_000L, totalAllocated())
    }

    @Test
    fun seedIfNeeded_keepsExistingDataUntouched() = runTest {
        database.seed(
            envelopes = listOf(RoomTestData.envelope(name = "已有信封")),
        )

        seeder.seedIfNeeded()

        assertEquals(1, database.seedDao().envelopeCount())
        assertEquals("已有信封", database.envelopeDao().get(RoomTestData.envelopeA)?.name)
    }
}
