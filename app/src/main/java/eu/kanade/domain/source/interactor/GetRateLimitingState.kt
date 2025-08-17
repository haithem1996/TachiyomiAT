package eu.kanade.domain.source.interactor

import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class GetRateLimitingState(
    private val basePreferences: BasePreferences,
    private val sourcePreferences: SourcePreferences,
    private val extensionManager: ExtensionManager,
) {
    fun await(sourceId: Long?): Boolean {
        if (basePreferences.rateLimiting().get()) return true
        if (sourceId == null) return false
        val extensionPackage = extensionManager.getExtensionPackage(sourceId) ?: return false

        return extensionPackage in sourcePreferences.rateLimitingExtensions().get()
    }

    fun subscribe(sourceId: Long?): Flow<Boolean> {
        if (sourceId == null) return basePreferences.rateLimiting().changes()

        return combine(
            basePreferences.rateLimiting().changes(),
            sourcePreferences.rateLimitingExtensions().changes(),
            extensionManager.getExtensionPackageAsFlow(sourceId),
        ) { rateLimiting, rateLimitingExtensions, extensionPackage ->
            rateLimiting || (extensionPackage in rateLimitingExtensions)
        }
            .distinctUntilChanged()
    }
}
