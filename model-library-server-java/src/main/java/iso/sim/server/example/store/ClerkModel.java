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
 * Modified by Sim Model Library contributors: isolated from the upstream DEVS framework and ports
 * so the non-production example does not bring a Store artifact dependency; see EXAMPLE_STORE_SOURCES.md.
 */

package iso.sim.server.example.store;

import java.util.List;

/**
 * Source-derived local Clerk example. It is not an implementation of the external DEVS runtime.
 */
public class ClerkModel {
    private final String modelIdentifier;
    private final ClerkState modelState;

    public ClerkModel(String modelIdentifier) {
        this.modelIdentifier = modelIdentifier;
        this.modelState = new ClerkState(0.0);
    }

    public String getModelIdentifier() {
        return modelIdentifier;
    }

    public ClerkState getModelState() {
        return modelState;
    }

    public void internalStateTransitionFunction() {
        double currentTime = modelState.getCurrentTime() + timeAdvanceFunction();
        modelState.setCurrentTime(currentTime);
        if (!modelState.getCustomerList().isEmpty()) {
            modelState.getCustomerList().removeFirst();
        }
        if (!modelState.getCustomerList().isEmpty()) {
            serveNextCustomer(currentTime);
        }
    }

    public void externalStateTransitionFunction(double elapsedTime, List<Customer> inputs) {
        double currentTime = modelState.getCurrentTime() + elapsedTime;
        modelState.setCurrentTime(currentTime);
        for (Customer customer : inputs) {
            modelState.getCustomerList().add(customer);
            if (modelState.getCustomerList().size() == 1) {
                serveNextCustomer(currentTime);
            }
        }
    }

    public void confluentStateTransitionFunction(List<Customer> inputs) {
        internalStateTransitionFunction();
        externalStateTransitionFunction(0.0, inputs);
    }

    public double timeAdvanceFunction() {
        if (modelState.getCustomerList().isEmpty()) {
            return Double.MAX_VALUE;
        }
        Customer nextCustomer = modelState.getCustomerList().getFirst();
        return nextCustomer.tleave() - modelState.getCurrentTime();
    }

    public List<Customer> outputFunction() {
        return List.of(modelState.getCustomerList().getFirst());
    }

    private void serveNextCustomer(double currentTime) {
        Customer nextCustomer = modelState.getCustomerList().removeFirst();
        modelState.getCustomerList().addFirst(nextCustomer.withTleave(currentTime + nextCustomer.twait()));
    }
}
