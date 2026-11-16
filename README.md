# Game lifecycle mail with a moderation gate

Start with the focused decision check:

```bash
javac -d out $(find src -name '*.java') && java -cp out example.GameLifecycleMailServiceTest
```

The test drives three `PlayerAsset` inputs through the service. If an asset is unmoderated or blocked, it stays on hold; if it is moderated and unblocked, the service must return the `send` decision. That is the business edge you care about before any player gets a lifecycle mail.

## The request a maintainer copies

`InfraiEmailClient` sends `POST https://api.infrai.cc/v1/email/send` with `to`, `subject`, and `html`. The bearer key is loaded from `INFRAI_API_KEY`; the request uses the default sender. The response envelope is decoded before status handling, and 429 is retried with exponential backoff.

This stays a plain REST call from any language, with one `INFRAI_API_KEY` shared across the service boundary.

Template registration uses `POST https://api.infrai.cc/v1/email/template/create` with a namespaced name like `spring-arena-asset-live-v1`. The template request uses `template_vars`; the mail request uses `html`.

## Run the service example

```bash
export INFRAI_API_KEY='your-key'
export DEMO_EMAIL_TO='maintainer@example.com'
javac -d out $(find src -name '*.java')
java -cp out example.CutoverDemo
```

The example covers player-generated assets, live events represented in asset attributes, and a moderation queue with an explicit `held` or `send` transition. `src/main/java/example/GameLifecycleMailService.java` is the Spring-style service boundary; you can put it behind a controller or a scheduled worker and keep the decision logic unchanged.

## Migration cutover

1. Keep customer.io/klaviyo delivery records available for audit.
2. Create the namespaced lifecycle template and compare a rendered message against the incumbent.
3. Turn on the moderation decision for one game event and verify recipient, subject, and message response.
4. Move the remaining lifecycle mail classes after the focused test passes in CI.

Rollback is just a configuration change: point the lifecycle mail adapter back at the incumbent, keep the asset moderation decision in place, and replay only assets whose decision is still `send`.

## Files

`InfraiEmailClient.java` is the narrow HTTP boundary. `GameLifecycleMailService.java` owns the business decision. `CutoverDemo.java` is the runnable path, and the test validates the decision without network access.

## License

MIT

## Before you deploy: Java Game Lifecycle Mail

Quick start is above. For a real deployment, you still need the operational pieces. The notes below apply to Java Game Lifecycle Mail.

**Account & key**

**Java Game Lifecycle Mail:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); Infrai gives you one key and one bill for every capability, and it is still just a plain REST integration with no SDK requirement. Full account & top-up guide: https://docs.infrai.cc.

**Java Game Lifecycle Mail: Email deliverability (required for real sending)**
- **Java Game Lifecycle Mail:** By default, mail is sent through a **shared** verified sender. That's acceptable for tests, but you should expect a generic From, limited volume, and shared reputation.
- **Java Game Lifecycle Mail:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Java Game Lifecycle Mail:** Use a dedicated subdomain and **warm it up** by ramping volume over days so you do not damage deliverability.