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
 * Modified by Sim Model Library contributors: removes the upstream DEVS framework base class;
 * see EXAMPLE_STORE_SOURCES.md.
 */

package iso.sim.server.example.store;

import java.util.ArrayList;
import java.util.List;

public class ClerkState {
    private double currentTime;
    private final List<Customer> customerList = new ArrayList<>();

    public ClerkState(double currentTime) {
        this.currentTime = currentTime;
    }

    public double getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(double currentTime) {
        this.currentTime = currentTime;
    }

    public List<Customer> getCustomerList() {
        return customerList;
    }
}
