/*
 * Sim Model Library Copyright (C) 2026 simlytics.cloud LLC and
 * Sim Model Library contributors.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License
 *  is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing permissions and limitations under
 *  the License.
 *
 */

package iso.sim.server.catalog;

import iso.sim.server.dto.catalog.ModelCatalogDto;
import iso.sim.server.dto.catalog.ModelListResponse;
import iso.sim.server.dto.catalog.ModelDto;
import iso.sim.server.dto.catalog.ModelSummaryDto;

import java.util.Comparator;
import java.util.List;

public class ModelCatalogService {
    private final ModelCatalogDto catalog;

    public ModelCatalogService(CatalogRepository repository) {
        this.catalog = repository.loadCatalog();
    }

    public ModelListResponse listModels() {
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
        return catalog.getModels().stream()
            .filter(model -> model.getModelId().equals(modelId))
            .findFirst()
            .orElseThrow(() -> new ModelNotFoundException(modelId));
    }
}
