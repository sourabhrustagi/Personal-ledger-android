package com.ledger.personal.data.local

import javax.inject.Inject
import javax.inject.Singleton

interface KycDraftStore {
    suspend fun save(name: String, confidence: Double, captureUri: String?)
    suspend fun load(): Triple<String, Double, String?>?
}

class MemoryKycDraftStore : KycDraftStore {
    private var saved: Triple<String, Double, String?>? = null

    override suspend fun save(name: String, confidence: Double, captureUri: String?) {
        saved = Triple(name, confidence, captureUri)
    }

    override suspend fun load(): Triple<String, Double, String?>? = saved
}

@Singleton
class RoomKycDraftStore @Inject constructor(private val dao: KycDraftDao) : KycDraftStore {
    override suspend fun save(name: String, confidence: Double, captureUri: String?) {
        dao.upsert(KycDraftEntity("draft", name, confidence, captureUri))
    }

    override suspend fun load(): Triple<String, Double, String?>? =
        dao.find("draft")?.let { Triple(it.name, it.confidence, it.captureUri) }
}
