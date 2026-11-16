package example;

import java.util.Map;

public final class CutoverDemo {
    public static void main(String[] args) throws Exception {
        String key = System.getenv("INFRAI_API_KEY");
        String recipient = System.getenv("DEMO_EMAIL_TO");
        if (key == null || recipient == null) throw new IllegalArgumentException("INFRAI_API_KEY and DEMO_EMAIL_TO are required");
        GameLifecycleMailService service = new GameLifecycleMailService(new InfraiEmailClient(key));
        var asset = new GameLifecycleMailService.PlayerAsset(recipient, "Spring arena badge", true, false,
                Map.of("event", "spring-arena"));
        System.out.println(service.sendApprovedAssetMail(asset));
    }
}
