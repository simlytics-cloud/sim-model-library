package iso.sim.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue

class CatalogRepository(
    private val resourcePath: String = "model-catalog.json",
    private val objectMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
) {
    fun loadCatalog(): ModelCatalogDto {
        val stream = javaClass.classLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException("Model catalog resource '$resourcePath' was not found")
        stream.use {
            val catalog: ModelCatalogDto = objectMapper.readValue(it)
            validate(catalog)
            return catalog
        }
    }

    private fun validate(catalog: ModelCatalogDto) {
        if (catalog.models.isEmpty()) {
            throw IllegalStateException("Model catalog must contain at least one model")
        }
        catalog.models.forEach { model ->
            if (model.modelId.isBlank()) {
                throw IllegalStateException("Each model must define a non-empty modelId")
            }
            if (model.name.isBlank()) {
                throw IllegalStateException("Model '${model.modelId}' must define a non-empty name")
            }
        }
    }
}

class ModelNotFoundException(modelId: String) : RuntimeException("Model '$modelId' was not found")
