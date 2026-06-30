package iso.sim.server.catalog;

import iso.sim.server.dto.catalog.AtomicModelDto;
import iso.sim.server.dto.catalog.AtomicModelSummaryDto;
import iso.sim.server.dto.catalog.ModelCatalogDto;
import iso.sim.server.dto.catalog.ModelListResponse;

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
        List<AtomicModelSummaryDto> summaries = catalog.getModels().stream()
            .sorted(Comparator.comparing(AtomicModelDto::getModelId))
            .map(it -> new AtomicModelSummaryDto(
                it.getModelId(),
                it.getName(),
                it.getDescription(),
                it.getImplementationLanguage()
            ))
            .toList();
        return new ModelListResponse(summaries);
    }

    public AtomicModelDto getModel(String modelId) {
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