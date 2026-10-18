package com.ledger.personal.data.local

import javax.inject.Inject
import javax.inject.Singleton

/** One saved KYC draft. */
data class KycDraft(val name: String, val confidence: Double, val captureUri: String?)

/** Contract for kyc draft store. */
interface KycDraftStore {
    /** Save. */
    suspend fun save(name: String, confidence: Double, captureUri: String?)
    /** Load. */
    suspend fun load(): KycDraft?
}

/** Memory kyc draft store. */
class MemoryKycDraftStore : KycDraftStore {
    private var saved: KycDraft? = null

    /** Save. */
    override suspend fun save(name: String, confidence: Double, captureUri: String?) {
        saved = KycDraft(name, confidence, captureUri)
    }

    /** Load. */
    override suspend fun load(): KycDraft? = saved
}

/** Room kyc draft store. */
@Singleton
class RoomKycDraftStore @Inject constructor(private val dao: KycDraftDao) : KycDraftStore {
    /** Save. */
    override suspend fun save(name: String, confidence: Double, captureUri: String?) {
        dao.upsert(KycDraftEntity("draft", name, confidence, captureUri))
    }

    /** Load. */
    override suspend fun load(): KycDraft? =
        dao.find("draft")?.let { KycDraft(it.name, it.confidence, it.captureUri) }
}
