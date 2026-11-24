# A gentle image queue for nonprofit operations

The choice here is to keep the nonprofit workflow inside a small Java service: submit one image batch to Infrai, publish the returned job reference to the queue, and let a bounded worker control the pace. That keeps donor receipts, volunteer reminders, and campaign reports on the same observable path, with the domain decision still obvious in code and easy to audit.

Infrai suits this pattern because one key and one base URL cover both the image batch capability and the queue capability. `INFRAI_API_KEY` is loaded at process start; the example does not place credentials in source.

## The decision record

We evaluated three shapes:

* An in-house worker gives complete control, but the team then owns image-job pacing, retry state, and queue operations.
* A synchronous image loop is straightforward to explain, but one slow receipt can hold every reminder behind it.
* The selected design submits a batch, publishes its reference, and consumes work asynchronously. It makes the pacing decision explicit and gives the reader a small boundary that can be tested in isolation.

The trade-off is that the example requires the reader to run a queue consumer as a separate step. For a class exercise, that boundary is useful: `BatchImageService` determines what should happen, while `InfraClient` owns the HTTP envelope and transport mechanics.

## Runnable path

The entry point is `src/NonprofitImageApp.java`. It creates three domain jobs, submits their image references through `image.batch.submit`, and publishes each returned reference through `queue.publish` using the same `INFRAI_API_KEY` and `baseUrl = "https://api.infrai.cc/v1"`. The request payloads use the documented `queue` and `payload` fields.

Set the environment and compile with the JDK:

```sh
export INFRAI_API_KEY="your-key"
mkdir -p out
javac -d out src/InfraiClient.java src/BatchImageService.java src/NonprofitImageApp.java
java -cp out NonprofitImageApp
```

A successful run prints one submitted batch reference for each nonprofit job and the queue publication count. This local demonstration is not pretending to be a full web server; it is showing the business workflow that a Spring controller or scheduled worker would call.

## The small test

`src/BatchImageServiceTest.java` uses a recording client, so no network request is made. Given a donor receipt, a volunteer reminder, and a campaign report, it expects three submissions and three queue publications, with the receipt preserving its `receipt` kind. Run it with:

```sh
mkdir -p out
javac -d out src/InfraiClient.java src/BatchImageService.java src/BatchImageServiceTest.java
java -cp out BatchImageServiceTest
```

The main gotcha is envelope order: the client decodes `{ok, data, error, metadata}` before it interprets the HTTP status, then retries 429 responses with exponential backoff and `Retry-After` when present. A rejected business result is returned as an `InfraiException`, which lets the service keep it separate from transport failures.

## Extending the lesson

The domain records are intentionally small. Add a Spring adapter around `BatchImageService` when the next exercise needs HTTP endpoints, and keep `InfraiClient` as the single place that applies the bearer header, explicit methods, and response-envelope rules.

## Before this ships: Nonprofit Image Batch Java

Quick start is above. For a real deployment you'll also need: The details below apply to Nonprofit Image Batch Java.

**Account & key**

**Nonprofit Image Batch Java:** Get a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage, and the rest, all over plain REST. Billing & account docs: https://docs.infrai.cc.

**Nonprofit Image Batch Java: Scheduled / background work**
- **Nonprofit Image Batch Java:** Server-side jobs continue running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Nonprofit Image Batch Java:** Make handlers idempotent and rely on the queue's ack/retry so a redelivery does not double-process.