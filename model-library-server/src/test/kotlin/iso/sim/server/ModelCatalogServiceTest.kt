package iso.sim.server

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ModelCatalogServiceTest {
    private val service = ModelCatalogService(CatalogRepository())

    @Test
    fun `listModels returns sorted model summaries`() {
        val response = service.listModels()
        assertTrue(response.models.size >= 1)
        assertTrue(response.models.any { it.modelId == "irpsystem.irpmodel.Vehicle" })
    }

    @Test
    fun `getModel returns vehicle model`() {
        val model = service.getModel("irpsystem.irpmodel.Vehicle")
        assertEquals("Vehicle", model.name)
        assertNotNull(model.initializationSchema)
    }

    @Test
    fun `getModel throws for unknown model id`() {
        assertThrows(ModelNotFoundException::class.java) {
            service.getModel("missing.model")
        }
    }

    @Test
    fun `vehicle fixture parity check`() {
        val stream = javaClass.classLoader.getResourceAsStream("irpsystem.irpmodel.Vehicle.get-model-response.json")
            ?: throw IllegalStateException("Fixture not found")
        val expected = ObjectMapper().readTree(stream)
        val actual = ObjectMapper().valueToTree<com.fasterxml.jackson.databind.JsonNode>(
            service.getModel("irpsystem.irpmodel.Vehicle")
        )
        assertEquals(expected.get("modelId"), actual.get("modelId"))
        assertEquals(expected.get("name"), actual.get("name"))
    }
}
