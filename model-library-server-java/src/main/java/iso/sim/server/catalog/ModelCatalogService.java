package iso.sim.server.catalog;

import iso.sim.server.dto.catalog.ModelCatalogDto;
import iso.sim.server.dto.catalog.ModelListResponse;
import iso.sim.server.dto.catalog.ModelDto;
import iso.sim.server.dto.catalog.ModelSummaryDto;

import java.util.Comparator;
import java.util.List;

public class ModelCatalogService {
    private final CatalogRepository repository;
    private ModelCatalogDto catalog;

    public ModelCatalogService(CatalogRepository repository) {
        this.repository = repository;
    }

    public ModelListResponse listModels() {
        ensureCatalogLoaded();
        List<ModelSummaryDto> summaries = catalog.getModels().stream()
            .sorted(Comparator.comparing(ModelDto::getModelId))
            .map(it -> new ModelSummaryDto(
                it.getModelId(),
                it.getName(),
                it.getDescription(),
                it.getImplementationLanguage()
            ))
            .toList();
        return new ModelListResponse(summaries);
    }

    public ModelDto getModel(String modelId) {
        ensureCatalogLoaded();
        return catalog.getModels().stream()
            .filter(model -> model.getModelId().equals(modelId))
            .findFirst()
            .orElseThrow(() -> new ModelNotFoundException(modelId));
    }

    private void ensureCatalogLoaded() {
        if (catalog == null) {
            catalog = repository.loadCatalog();
        }
    }
}
