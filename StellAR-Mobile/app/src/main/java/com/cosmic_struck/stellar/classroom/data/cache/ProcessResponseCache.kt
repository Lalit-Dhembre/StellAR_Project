package com.cosmic_struck.stellar.classroom.data.cache

import com.cosmic_struck.stellar.classroom.data.dto.ProcessResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cache for ProcessResponse (quiz data).
 * This cache is cleared when the app process ends.
 * 
 * Uses module_id as cache key to store responses per module.
 */
@Singleton
class ProcessResponseCache @Inject constructor() {
    
    private val cache = mutableMapOf<Long, ProcessResponse>()
    
    /**
     * Get cached response for a module
     */
    fun get(moduleId: Long): ProcessResponse? {
        return cache[moduleId]
    }
    
    /**
     * Cache a response for a module
     */
    fun put(moduleId: Long, response: ProcessResponse) {
        cache[moduleId] = response
    }
    
    /**
     * Check if a response is cached for a module
     */
    fun contains(moduleId: Long): Boolean {
        return cache.containsKey(moduleId)
    }
    
    /**
     * Clear cache for a specific module
     */
    fun remove(moduleId: Long) {
        cache.remove(moduleId)
    }
    
    /**
     * Clear all cached responses
     */
    fun clearAll() {
        cache.clear()
    }
    
    /**
     * Get all cached module IDs
     */
    fun getCachedModuleIds(): Set<Long> {
        return cache.keys.toSet()
    }
}
