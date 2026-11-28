# Contributing

Start with the local demo in [English](README.en.md#start-the-storefront-and-admin-demo) or [中文](README.md#一键体验商城和后台). Use a demo database for shopping smoke tests, which create a test account and place an order against the seeded catalog.

Run backend tests from the repository root, then package the service JARs separately. The order contract tests use the inventory and payment modules' test-time classpaths, while the runnable JARs use Spring Boot packaging.

```bash
mvn -B -ntp test
mvn -B -ntp -Dmaven.test.skip=true package
```

With the demo running, test the shopping flow in Chromium:

```bash
cd ecommerce-web
npx playwright install chromium
npx playwright test tests/e2e/full-shopping-flow-real.spec.ts
```

Set `PLAYWRIGHT_VERIFY_OUTBOX=1` to also exercise recovery from an expired Outbox send lease against the demo's MySQL and RocketMQ. CI enables this check and attaches test results and application logs when it fails.

For a pull request, describe the change and how you verified it. Update both READMEs when setup commands or supported workflows change.
