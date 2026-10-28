package com.example.dukkanapp.features.auth.domain.repository

import com.example.dukkanapp.core.error.EmptyResult

interface DeviceRepository {
    /** Creates or refreshes users/{uid}/devices/{installationId} for this install. */
    suspend fun registerCurrentDevice(uid: String, languageCode: String): EmptyResult

    /** Deletes this install's device document so it stops receiving the user's pushes. */
    suspend fun unregisterCurrentDevice(uid: String): EmptyResult

    // TODO(push): suspend fun updatePushToken(uid: String, token: String): EmptyResult
}
