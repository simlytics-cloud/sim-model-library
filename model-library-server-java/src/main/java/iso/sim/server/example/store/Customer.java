/*
 * DEVS Streaming Framework Store Java Copyright (C) 2024 simlytics.cloud LLC and
 * DEVS Streaming Framework Store Java contributors.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 * Modified by Sim Model Library contributors: local replacement for the upstream generated
 * Customer implementation; see EXAMPLE_STORE_SOURCES.md.
 */

package iso.sim.server.example.store;

/**
 * Minimal local counterpart of the Store example's generated Customer type.
 */
public record Customer(double twait, double tenter, double tleave) {
    public Customer withTleave(double value) {
        return new Customer(twait, tenter, value);
    }
}
