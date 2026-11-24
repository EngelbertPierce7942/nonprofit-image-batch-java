import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BatchImageService implements AutoCloseable {
    public record WorkItem(String kind, String image) {}
    private final InfraiClient client;
    private final ExecutorService workers;
    private final String queue;

    public BatchImageService(InfraiClient client, String queue, int concurrency) {
        this.client = client; this.queue = queue; this.workers = Executors.newFixedThreadPool(concurrency);
    }

    public CompletableFuture<Integer> submit(List<WorkItem> items) {
        return CompletableFuture.supplyAsync(() -> {
            int published = 0;
            for (WorkItem item : items) {
                try {
                    String result = client.submitImageBatch(item.image(), item.kind() + "-batch");
                    client.publish(queue, result);
                    published++;
                } catch (IOException | InterruptedException e) { throw new RuntimeException(e); }
            }
            return published;
        }, workers);
    }

    @Override public void close() { workers.shutdown(); }
}
