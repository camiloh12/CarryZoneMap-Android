package com.carryzonemap.app.data.remote.datasource

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
import timber.log.Timber
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
            private const val TABLE_NAME = "pins"
        }

        override suspend fun getAllPins(): Result<List<Pin>> {
            return try {
                Timber.d("Fetching all pins from Supabase...")
                val response =
                    postgrest
                        .from(TABLE_NAME)
                        .select()
                        .decodeList<SupabasePinDto>()

                Timber.d("Fetched ${response.size} pins from Supabase")
                Result.success(response.toDomainModels())
            } catch (e: Exception) {
                Timber.e(e, "Error fetching all pins from Supabase: ${e.message}")
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
                Timber.e(e, "Error fetching pin by id: $pinId")
                Result.failure(e)
            }
        }

        override suspend fun insertPin(pin: Pin): Result<Pin> {
            return try {
                Timber.d("Inserting pin to Supabase: ${pin.id} at (${pin.location.longitude}, ${pin.location.latitude})")
                val dto = pin.toSupabaseDto()
                val response =
                    postgrest
                        .from(TABLE_NAME)
                        .insert(dto) {
                            select()
                        }.decodeSingle<SupabasePinDto>()

                Timber.d("Successfully inserted pin to Supabase: ${pin.id}")
                Result.success(response.toDomain())
            } catch (e: Exception) {
                Timber.e(e, "Error inserting pin ${pin.id} to Supabase: ${e.message}")
                Result.failure(e)
            }
        }

        override suspend fun updatePin(pin: Pin): Result<Pin> {
            return try {
                Timber.d("Updating pin in Supabase: ${pin.id}")
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

                Timber.d("Successfully updated pin in Supabase: ${pin.id}")
                Result.success(response.toDomain())
            } catch (e: Exception) {
                Timber.e(e, "Error updating pin ${pin.id} in Supabase: ${e.message}")
                Result.failure(e)
            }
        }

        override suspend fun deletePin(pinId: String): Result<Unit> {
            return try {
                Timber.d("Deleting pin from Supabase: $pinId")
                postgrest
                    .from(TABLE_NAME)
                    .delete {
                        filter {
                            eq("id", pinId)
                        }
                    }

                Timber.d("Successfully deleted pin from Supabase: $pinId")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Error deleting pin $pinId from Supabase: ${e.message}")
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
                Timber.e(e, "Error fetching pins in bounding box")
                Result.failure(e)
            }
        }

        override fun subscribeToChanges(): Flow<PinChangeEvent> =
            callbackFlow<PinChangeEvent> {
                Timber.d("Realtime subscriptions not yet implemented")
                Timber.d("Will use polling-based sync instead")

                // TODO: Implement realtime subscriptions in Phase 7
                // For now, return an empty flow
                // The sync manager will use polling instead

                awaitClose {
                    Timber.d("Closing realtime subscription flow")
                }
            }
    }
