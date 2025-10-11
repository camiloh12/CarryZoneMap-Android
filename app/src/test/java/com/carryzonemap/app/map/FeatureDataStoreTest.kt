package com.carryzonemap.app.map

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.geojson.Feature
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class FeatureDataStoreTest {
    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockPrefs: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    @Mock
    private lateinit var onDataChanged: (List<Feature>) -> Unit

    @Captor
    private lateinit var featureListCaptor: ArgumentCaptor<List<Feature>>

    @Captor
    private lateinit var stringCaptor: ArgumentCaptor<String>

    private lateinit var featureDataStore: FeatureDataStore

    @Before
    fun setUp() {
        // Mock the chain of calls to get to the SharedPreferences editor
        whenever(mockContext.getSharedPreferences(any(), any())).thenReturn(mockPrefs)
        whenever(mockPrefs.edit()).thenReturn(mockEditor)
        // Use lenient() for stubs that are part of general setup and may not be used in all tests.
        lenient().whenever(mockEditor.putString(any(), any())).thenReturn(mockEditor)
    }

    @Test
    fun `init with no saved data calls onDataChanged with empty list`() {
        // Arrange: No data in SharedPreferences
        whenever(mockPrefs.getString(any(), eq(null))).thenReturn(null)

        // Act
        featureDataStore = FeatureDataStore(mockContext, onDataChanged)

        // Assert
        verify(onDataChanged).invoke(capture(featureListCaptor))
        assertTrue(featureListCaptor.value.isEmpty())
    }

    @Test
    fun `init with saved data loads features and calls onDataChanged`() {
        // Arrange: Valid JSON data in SharedPreferences
        val json =
            JSONArray()
                .put(PersistedFeature("id1", 10.0, 20.0, 0).toJSONObject())
                .toString()
        whenever(mockPrefs.getString(any(), eq(null))).thenReturn(json)

        // Act
        featureDataStore = FeatureDataStore(mockContext, onDataChanged)

        // Assert
        verify(onDataChanged).invoke(capture(featureListCaptor))
        val features = featureListCaptor.value
        assertEquals(1, features.size)
        assertEquals("id1", features[0].getStringProperty("feature_id"))
    }

    @Test
    fun `addFeature saves feature and calls onDataChanged`() {
        // Arrange
        whenever(mockPrefs.getString(any(), eq(null))).thenReturn(null)
        featureDataStore = FeatureDataStore(mockContext, onDataChanged)

        // Act
        featureDataStore.addFeature(-122.0, 37.0)

        // Assert: Check that SharedPreferences.putString was called with the correct JSON
        verify(mockEditor).putString(any(), capture(stringCaptor))
        val savedJson = JSONArray(stringCaptor.value)
        assertEquals(1, savedJson.length())
        assertEquals(-122.0, savedJson.getJSONObject(0).getDouble("lon"), 0.0)

        // Assert: Check that onDataChanged was called with the new feature list
        // It's called once on init and once on add
        verify(onDataChanged, times(2)).invoke(capture(featureListCaptor))
        assertEquals(1, featureListCaptor.value.size)
    }

    @Test
    fun `cycleFeatureColorState updates feature and saves`() {
        // Arrange: Start with one feature in the store
        val initialJson =
            JSONArray()
                .put(PersistedFeature("id1", 10.0, 20.0, FeatureDataStore.COLOR_STATE_GREEN).toJSONObject())
                .toString()
        whenever(mockPrefs.getString(any(), eq(null))).thenReturn(initialJson)
        featureDataStore = FeatureDataStore(mockContext, onDataChanged)

        // Act
        val result = featureDataStore.cycleFeatureColorState("id1")

        // Assert
        assertTrue(result)

        // Assert: Check that the saved JSON reflects the new color state
        verify(mockEditor).putString(any(), capture(stringCaptor))
        val savedJson = JSONArray(stringCaptor.value)
        assertEquals(FeatureDataStore.COLOR_STATE_YELLOW, savedJson.getJSONObject(0).getInt("state"))

        // Assert: onDataChanged should have been called again
        verify(onDataChanged, times(2)).invoke(any())
    }

    @Test
    fun `cycleFeatureColorState with invalid id returns false`() {
        // Arrange
        // This stub is only used for object construction, so we mark it as lenient.
        lenient().whenever(mockPrefs.getString(any(), any())).thenReturn(null)
        featureDataStore = FeatureDataStore(mockContext, onDataChanged)

        // Act
        val result = featureDataStore.cycleFeatureColorState("invalid-id")

        // Assert
        assertFalse(result)
        // Verify save was NOT called
        verify(mockEditor, never()).putString(any(), any())
        // onDataChanged was only called once during init
        verify(onDataChanged, times(1)).invoke(any())
    }
}
