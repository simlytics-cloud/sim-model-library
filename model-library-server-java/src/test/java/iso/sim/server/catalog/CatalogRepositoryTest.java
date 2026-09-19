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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogRepositoryTest {
    @Test
    void loadsCatalogFromConfiguredFilesystemPath() {
        CatalogRepository repository = new CatalogRepository(
            List.of(testCatalogPath()),
            new ObjectMapper()
        );

        var catalog = repository.loadCatalog();

        assertEquals(1, catalog.getModels().size());
        assertEquals("irpsystem.irpmodel.Vehicle", catalog.getModels().get(0).getModelId());
    }

    @Test
    void identifiesMissingConfiguredCatalogFile() {
        CatalogRepository repository = new CatalogRepository(
            List.of("does-not-exist-model-catalog.json"),
            new ObjectMapper()
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, repository::loadCatalog);

        assertTrue(exception.getMessage().contains("does-not-exist-model-catalog.json"));
        assertTrue(exception.getMessage().contains("was not found"));
    }

    @Test
    void loadsDefaultCatalogConfiguredInApplicationConf() {
        var config = ConfigFactory.load();
        var catalogPaths = config.getStringList("model.library.catalog.resources");

        var catalog = new CatalogRepository(catalogPaths, new ObjectMapper()).loadCatalog();

        assertEquals(List.of("classpath:data/model-catalog.json"), catalogPaths);
        assertEquals("irpsystem.irpmodel.Vehicle", catalog.getModels().get(0).getModelId());
    }

    private static String testCatalogPath() {
        return Path.of(Objects.requireNonNull(
            CatalogRepositoryTest.class.getResource("/model-catalog.json")
        ).getPath()).toString();
    }
}
