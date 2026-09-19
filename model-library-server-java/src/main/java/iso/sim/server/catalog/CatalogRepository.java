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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import iso.sim.server.dto.catalog.ModelCatalogDto;
import iso.sim.server.dto.catalog.ModelDto;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

public class CatalogRepository {
    private final List<String> catalogPaths;
    private final ObjectMapper objectMapper;

    public CatalogRepository(List<String> catalogPaths) {
        this(catalogPaths, new ObjectMapper());
    }

    public CatalogRepository(List<String> catalogPaths, ObjectMapper objectMapper) {
        this.catalogPaths = catalogPaths;
        this.objectMapper = objectMapper;
    }

    public ModelCatalogDto loadCatalog() {
        List<ModelDto> allModels = catalogPaths.stream().flatMap(path -> {
            try (InputStream is = stream(path)) {
                ModelCatalogDto catalog = objectMapper.readValue(is, new TypeReference<>() {});
                return catalog.getModels().stream();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load model catalog file '" + path + "'", e);
            }
        }).toList();

        ModelCatalogDto catalog = new ModelCatalogDto(allModels);
        validate(catalog);
        return catalog;
    }

    private InputStream stream(String path) {
        if (path.startsWith("classpath:")) {
            String resourcePath = path.substring("classpath:".length());
            InputStream resource = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (resource == null) {
                throw new IllegalStateException("Model catalog resource '" + path + "' was not found");
            }
            return resource;
        }

        try {
            return Files.newInputStream(Path.of(path));
        } catch (NoSuchFileException e) {
            throw new IllegalStateException("Model catalog file '" + path + "' was not found", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open model catalog file '" + path + "'", e);
        }
    }

    private void validate(ModelCatalogDto catalog) {
        if (catalog.getModels().isEmpty()) {
            throw new IllegalStateException("Model catalog must contain at least one model");
        }
        for (ModelDto model : catalog.getModels()) {
            if (model.getModelId().isBlank()) {
                throw new IllegalStateException("Each model must define a non-empty modelId");
            }
            if (model.getName().isBlank()) {
                throw new IllegalStateException("Model '" + model.getModelId() + "' must define a non-empty name");
            }
        }
    }
}
