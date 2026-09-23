# Local example-store provenance

`ClerkModel.java` is derived from
[`ClerkModel.java`](https://github.com/simlytics-cloud/devs-sf-store-java/blob/bd444f708a53daaa6baaf1b358d74f2896b57bea/src/main/java/cloud/simlytics/devssfstore/ClerkModel.java),
whose source blob is `bd444f708a53daaa6baaf1b358d74f2896b57bea`.

`ClerkState.java` and the customer shape are the minimum direct supporting sources, derived from
the upstream `ClerkState.java` and `AbstractCustomer.java` in the same repository. The upstream
project is Apache-2.0 licensed. The local copies preserve its attribution and are intentionally
adapted to remove DEVS Streaming Framework and generated-Immutable dependencies. They are only
used by the isolated non-production `example-clerk` executor; they do not assert external DEVS or
Kafka runtime compatibility.
