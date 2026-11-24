import java.util.ArrayList;
import java.util.List;

public final class BatchImageServiceTest {
    private static final class RecordingClient extends InfraiClient {
        final List<String> submitted = new ArrayList<>();
        final List<String> published = new ArrayList<>();
        RecordingClient() { super("test-key"); }
        @Override public String submitImageBatch(String image, String clientId) { submitted.add(image + ":" + clientId); return "batch-" + submitted.size(); }
        @Override public String publish(String queue, String payload) { published.add(queue + ":" + payload); return "published"; }
    }

    public static void main(String[] args) {
        RecordingClient client = new RecordingClient();
        try (BatchImageService service = new BatchImageService(client, "test-images", 1)) {
            int count = service.submit(List.of(
                    new BatchImageService.WorkItem("receipt", "donor.jpg"),
                    new BatchImageService.WorkItem("volunteer-reminder", "reminder.jpg"),
                    new BatchImageService.WorkItem("campaign-report", "report.jpg"))).join();
            assert count == 3 : count;
            assert client.submitted.get(0).equals("donor.jpg:receipt-batch") : client.submitted;
            assert client.published.size() == 3 : client.published;
        }
        System.out.println("business decision test passed");
    }
}
