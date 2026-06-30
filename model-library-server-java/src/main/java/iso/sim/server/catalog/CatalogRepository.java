package iso.sim.server.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import iso.sim.server.dto.catalog.AtomicModelDto;
import iso.sim.server.dto.catalog.ModelCatalogDto;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class CatalogRepository {
    private final List<String> resourcePaths;
    private final ObjectMapper objectMapper;

    public CatalogRepository() {
        this(List.of("model-catalog.json", "bifrost-model-catalog.json"), new ObjectMapper());
    }

    public CatalogRepository(List<String> resourcePaths, ObjectMapper objectMapper) {
        this.resourcePaths = resourcePaths;
        this.objectMapper = objectMapper;
    }

    public ModelCatalogDto loadCatalog() {
        List<AtomicModelDto> allModels = resourcePaths.stream().flatMap(path -> {
            InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
            if (stream == null) {
                throw new IllegalStateException("Model catalog resource '" + path + "' was not found");
            }
            try (InputStream is = stream) {
                ModelCatalogDto catalog = objectMapper.readValue(is, new TypeReference<>() {});
                return catalog.getModels().stream();
            } catch (IOException e) {
                throw new RuntimeException("Failed to load model catalog resource '" + path + "'", e);
            }
        }).toList();

        ModelCatalogDto catalog = new ModelCatalogDto(allModels);
        validate(catalog);
        return catalog;
    }

    private void validate(ModelCatalogDto catalog) {
        if (catalog.getModels().isEmpty()) {
            throw new IllegalStateException("Model catalog must contain at least one model");
        }
        for (AtomicModelDto model : catalog.getModels()) {
            if (model.getModelId().isBlank()) {
                throw new IllegalStateException("Each model must define a non-empty modelId");
            }
            if (model.getName().isBlank()) {
                throw new IllegalStateException("Model '" + model.getModelId() + "' must define a non-empty name");
            }
        }
    }
}