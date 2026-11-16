package example;

import java.io.IOException;
import java.util.Map;

public final class GameLifecycleMailService {
    private final InfraiEmailClient email;

    public GameLifecycleMailService(InfraiEmailClient email) { this.email = email; }

    public Decision handleAssetModeration(PlayerAsset asset) {
        if (!asset.moderated()) return new Decision("held", "moderation_queue");
        if (asset.blocked()) return new Decision("held", "policy_review");
        return new Decision("send", "asset_approved");
    }

    public String sendApprovedAssetMail(PlayerAsset asset) throws IOException, InterruptedException {
        Decision decision = handleAssetModeration(asset);
        if (!decision.action().equals("send")) return decision.reason();
        String html = "<p>Your event asset <b>" + asset.title() + "</b> is live.</p>";
        return email.send(asset.playerEmail(), "Your game asset is live", html);
    }

    public String registerLifecycleTemplate(String namespace) throws IOException, InterruptedException {
        String name = namespace + "-asset-live-v1";
        return email.createTemplate(name, "Your game asset is live", "<p>{{asset_title}} is live.</p>");
    }

    public record PlayerAsset(String playerEmail, String title, boolean moderated, boolean blocked,
                              Map<String, String> attributes) { }
    public record Decision(String action, String reason) { }
}
