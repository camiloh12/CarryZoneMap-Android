package com.carryzonemap.app.data.remote.datasource

import android.util.Log
import com.carryzonemap.app.data.remote.dto.SupabasePinDto
import com.carryzonemap.app.data.remote.mapper.SupabaseMapper.toDomain
import com.carryzonemap.app.data.remote.mapper.SupabaseMapper.toDomainModels
import com.carryzonemap.app.data.remote.mapper.SupabaseMapper.toSupabaseDto
import com.carryzonemap.app.domain.model.Pin
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase implementation of RemotePinDataSource.
 *
 * Provides CRUD operations and real-time subscriptions for pins using Supabase.
 *
 * @property postgrest Supabase Postgrest client for database operations
 * @property realtime Supabase Realtime client for subscriptions
 */
@Singleton
class SupabasePinDataSource
    @Inject
    constructor(
        private val postgrest: Postgrest,
        private val realtime: Realtime,
    ) : RemotePinDataSource {
        companion object {
            private const val TAG = "SupabasePinDataSource"
            private const val TABLE_NAME = "pins"
        }

        override suspend fun getAllPins(): Result<List<Pin>> {
            return try {
                val response =
                    postgrest
                        .from(TABLE_NAME)
                        .select()
                        .decodeList<SupabasePinDto>()

                Result.success(response.toDomainModels())
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching all pins", e)
                Result.failure(e)
            }
        }

        override suspend fun getPinById(pinId: String): Result<Pin?> {
            return try {
                val response =
                    postgrest
                        .from(TABLE_NAME)
                        .select {
                            filter {
                                eq("id", pinId)
                            }
                        }.decodeSingleOrNull<SupabasePinDto>()

                Result.success(response?.toDomain())
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching pin by id: $pinId", e)
                Result.failure(e)
            }
        }

        override suspend fun insertPin(pin: Pin): Result<Pin> {
            return try {
                val dto = pin.toSupabaseDto()
                val response =
                    postgrest
                        .from(TABLE_NAME)
                        .insert(dto) {
                            select()
                        }.decodeSingle<SupabasePinDto>()

                Result.success(response.toDomain())
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting pin", e)
                Result.failure(e)
            }
        }

        override suspend fun updatePin(pin: Pin): Result<Pin> {
            return try {
                val dto = pin.toSupabaseDto()
                val response =
                    postgrest
                        .from(TABLE_NAME)
                        .update(dto) {
                            filter {
                                eq("id", pin.id)
                            }
                            select()
                        }.decodeSingle<SupabasePinDto>()

                Result.success(response.toDomain())
            } catch (e: Exception) {
                Log.e(TAG, "Error updating pin: ${pin.id}", e)
                Result.failure(e)
            }
        }

        override suspend fun deletePin(pinId: String): Result<Unit> {
            return try {
                postgrest
                    .from(TABLE_NAME)
                    .delete {
                        filter {
                            eq("id", pinId)
                        }
                    }

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting pin: $pinId", e)
                Result.failure(e)
            }
        }

        override suspend fun getPinsInBoundingBox(
            minLat: Double,
            maxLat: Double,
            minLng: Double,
            maxLng: Double,
        ): Result<List<Pin>> {
            return try {
                val response =
                    postgrest
                        .from(TABLE_NAME)
                        .select {
                            filter {
                                gte("latitude", minLat)
                                lte("latitude", maxLat)
                                gte("longitude", minLng)
                                lte("longitude", maxLng)
                            }
                        }.decodeList<SupabasePinDto>()

                Result.success(response.toDomainModels())
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching pins in bounding box", e)
                Result.failure(e)
            }
        }

        override fun subscribeToChanges(): Flow<PinChangeEvent> =
            callbackFlow<PinChangeEvent> {
                Log.d(TAG, "Realtime subscriptions not yet implemented")
                Log.d(TAG, "Will use polling-based sync instead")

                // TODO: Implement realtime subscriptions in Phase 7
                // For now, return an empty flow
                // The sync manager will use polling instead

                awaitClose {
                    Log.d(TAG, "Closing realtime subscription flow")
                }
            }
    }
