package com.quranicwords.app.core.data.datastore

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserPreferencesDataStoreTest {

    private lateinit var dataStore: UserPreferencesDataStore

    @Before
    fun setUp() {
        dataStore = UserPreferencesDataStore(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `getOrCreateLocalUserId returns stable id on repeated calls`() = runTest {
        val id1 = dataStore.getOrCreateLocalUserId()
        val id2 = dataStore.getOrCreateLocalUserId()
        assertNotNull(id1)
        assertEquals(id1, id2)
    }

    @Test
    fun `concurrent calls to getOrCreateLocalUserId return identical id without race condition`() = runTest {
        val deferreds = (1..10).map {
            async { dataStore.getOrCreateLocalUserId() }
        }
        val ids = deferreds.awaitAll()
        val distinctIds = ids.toSet()
        assertEquals(1, distinctIds.size)
    }

    @Test
    fun `setLocalUserId updates id and is returned by getOrCreateLocalUserId`() = runTest {
        dataStore.setLocalUserId("custom_user_123")
        val retrieved = dataStore.getOrCreateLocalUserId()
        assertEquals("custom_user_123", retrieved)
    }
}
