package com.savedex.core.domain.usecase

import com.savedex.core.domain.SaveRepository
import com.savedex.core.domain.SaveVersion
import kotlinx.coroutines.flow.Flow

/** Versions of a slot, in whatever order the repository returns them (newest first). */
class ListSaveVersionsUseCase(private val repository: SaveRepository) {
    operator fun invoke(slotId: String): Flow<List<SaveVersion>> = repository.observeVersions(slotId)
}
