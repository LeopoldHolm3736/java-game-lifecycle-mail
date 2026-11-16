package example;

import java.util.Map;

public final class GameLifecycleMailServiceTest {
    public static void main(String[] args) {
        InfraiEmailClient client = new InfraiEmailClient("test-key");
        GameLifecycleMailService service = new GameLifecycleMailService(client);
        var queued = new GameLifecycleMailService.PlayerAsset("player@example.com", "Dragon card", false, false, Map.of());
        var blocked = new GameLifecycleMailService.PlayerAsset("player@example.com", "Unsafe card", true, true, Map.of());
        var approved = new GameLifecycleMailService.PlayerAsset("player@example.com", "Forest card", true, false, Map.of("event", "spring"));
        check(service.handleAssetModeration(queued).action().equals("held"), "unmoderated assets are held");
        check(service.handleAssetModeration(blocked).reason().equals("policy_review"), "blocked assets enter policy review");
        check(service.handleAssetModeration(approved).action().equals("send"), "approved assets are sent");
        System.out.println("GameLifecycleMailServiceTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
