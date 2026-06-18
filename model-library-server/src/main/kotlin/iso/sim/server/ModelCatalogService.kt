package iso.sim.server

class ModelCatalogService(
    private val repository: CatalogRepository
) {
    private val catalog: ModelCatalogDto by lazy { repository.loadCatalog() }

    fun listModels(): ModelListResponse {
        val summaries = catalog.models
            .sortedBy { it.modelId }
            .map {
                AtomicModelSummaryDto(
                    modelId = it.modelId,
                    name = it.name,
                    description = it.description,
                    implementationLanguage = it.implementationLanguage
                )
            }
        return ModelListResponse(summaries)
    }

    fun getModel(modelId: String): AtomicModelDto {
        return catalog.models.firstOrNull { it.modelId == modelId }
            ?: throw ModelNotFoundException(modelId)
    }
}
