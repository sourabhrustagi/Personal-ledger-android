package com.ledger.personal.data.local

import javax.inject.Inject
import javax.inject.Singleton

/** Contract for kyc draft store. */
interface KycDraftStore {
    /** Save. */
    suspend fun save(name: String, confidence: Double, captureUri: String?)
    /** Load. */
    suspend fun load(): Triple<String, Double, String?>?
}

/** Memory kyc draft store. */
class MemoryKycDraftStore : KycDraftStore {
    private var saved: Triple<String, Double, String?>? = null

    /** Save. */
    override suspend fun save(name: String, confidence: Double, captureUri: String?) {
        saved = Triple(name, confidence, captureUri)
    }

    /** Load. */
    override suspend fun load(): Triple<String, Double, String?>? = saved
}

/** Room kyc draft store. */
@Singleton
class RoomKycDraftStore @Inject constructor(private val dao: KycDraftDao) : KycDraftStore {
    /** Save. */
    override suspend fun save(name: String, confidence: Double, captureUri: String?) {
        dao.upsert(KycDraftEntity("draft", name, confidence, captureUri))
    }

    /** Load. */
    override suspend fun load(): Triple<String, Double, String?>? =
        dao.find("draft")?.let { Triple(it.name, it.confidence, it.captureUri) }
}
