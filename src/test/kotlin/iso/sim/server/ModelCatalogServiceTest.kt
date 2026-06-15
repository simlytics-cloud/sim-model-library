package iso.sim.server

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ModelCatalogServiceTest {
    private val service = ModelCatalogService(CatalogRepository())

    @Test
    fun `listModels returns sorted model summaries`() {
        val response = service.listModels()
        assertEquals(1, response.models.size)
        assertEquals("irpsystem.irpmodel.Vehicle", response.models.first().modelId)
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
        val expected = ObjectMapper().readTree(
            java.io.File("irpsystem.irpmodel.Vehicle.get-model-response.json").inputStream()
        )
        val actual = ObjectMapper().valueToTree<com.fasterxml.jackson.databind.JsonNode>(
            service.getModel("irpsystem.irpmodel.Vehicle")
        )
        assertEquals(expected.get("modelId"), actual.get("modelId"))
        assertEquals(expected.get("name"), actual.get("name"))
    }
}
