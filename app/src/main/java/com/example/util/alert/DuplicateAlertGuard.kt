package com.example.util.alert

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DuplicateAlertGuard {

    private val seenEvents = ConcurrentHashMap<String, Long>()
    private val maxCacheSize = 2000
    private val defaultTtlMillis = TimeUnit.HOURS.toMillis(12)

    /**
     * Constructs a deterministic, unique event key following the required structure:
     * {broker}:{account}:{orderId}:{positionId}:{eventType}:{targetNumber}:{timestampBucket}
     */
    fun buildEventKey(
        broker: String = "ALL",
        account: String = "DEFAULT",
        orderId: String = "",
        positionId: String = "",
        eventType: AlertEventType,
        targetNumber: Int = 0,
        timestampBucket: Long = 0L
    ): String {
        return "$broker:$account:$orderId:$positionId:${eventType.key}:$targetNumber:$timestampBucket"
    }

    /**
     * Checks if this event has already been dispatched.
     * If not seen, records the event and returns true (allowed to send).
     * If already seen, returns false (blocked).
     */
    @Synchronized
    fun tryAcquire(eventKey: String, ttlMillis: Long = defaultTtlMillis): Boolean {
        cleanupExpired()
        val now = System.currentTimeMillis()
        val existing = seenEvents[eventKey]
        if (existing != null && (now - existing) < ttlMillis) {
            return false // Duplicate blocked
        }

        if (seenEvents.size >= maxCacheSize) {
            // Evict oldest entries
            val oldest = seenEvents.entries.sortedBy { it.value }.take(500)
            oldest.forEach { seenEvents.remove(it.key) }
        }

        seenEvents[eventKey] = now
        return true
    }

    /**
     * Clears all seen keys (e.g. on new trading session or test reset).
     */
    fun clear() {
        seenEvents.clear()
    }

    private fun cleanupExpired() {
        val now = System.currentTimeMillis()
        val iterator = seenEvents.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > defaultTtlMillis) {
                iterator.remove()
            }
        }
    }
}
