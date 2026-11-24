import java.util.List;

public final class NonprofitImageApp {
    public static void main(String[] args) {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        try (BatchImageService service = new BatchImageService(new InfraiClient(key), "nonprofit-images", 2)) {
            int count = service.submit(List.of(
                    new BatchImageService.WorkItem("receipt", "receipt-2026-01.jpg"),
                    new BatchImageService.WorkItem("volunteer-reminder", "volunteer-2026-01.jpg"),
                    new BatchImageService.WorkItem("campaign-report", "campaign-2026-01.jpg"))).join();
            System.out.println("Published " + count + " nonprofit image jobs");
        }
    }
}
