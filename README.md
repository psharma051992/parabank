# Bank Fund transfer automation (ParaBank)

Maven + TestNG + Selenium test for the banking fund-transfer scenario on https://parabank.parasoft.com/.

A new customer registers, opens a second account, transfers money, and the test checks that the transaction history and both balances match that transfer.

Page objects live under `src/test/java/com/fundtransfer/pages`. They fill forms, click links, and read the page. The checks (`Assert`) are in `FundTransferTest`.

## What you need

- JDK 11 or newer (`JAVA_HOME` must point at the JDK, not just a `java` on the PATH)
- Google Chrome
- Maven 3.9, or use the wrapper checked in with this project

## Run

Chrome opens on screen:

```bash
mvn test
```

If Maven is not installed:

```bash
mvnw.cmd test
```

Headless:

```bash
mvn test -Dheadless=true
```

## Report and logs

TestNG writes the report to `target/surefire-reports/index.html` and `target/surefire-reports/emailable-report.html`.

Balance snapshots are also printed on the console and stored on the TestNG report through `Reporter.log`. Failures include the rows the activity page actually showed.

Waits are `WebDriverWait` (explicit waits). There is no `Thread.sleep`.

## Assumptions

- Registering a customer also creates one checking account. The starting balance is whatever the demo is configured with. The test reads it from Accounts Overview.
- Opening a savings account moves the site's minimum deposit out of that checking account. That happens before the "before transfer" snapshot.
- This test then transfers $50 from the checking account to the new savings account.
- Usernames cannot be reused on this site, so each run builds one from the current time (`alex` plus a timestamp).
- A transfer is stored as "Funds Transfer Sent" on the account money left, and "Funds Transfer Received" on the account money arrived in. Opening the savings account uses those same words for the minimum deposit, so the check also looks for `$50.00`.
- Login details in the test are made-up demo data. The sample user printed on the ParaBank home page (`john` / `demo`) is not used, because the scenario starts with registration.

## If a run fails

Open `target/surefire-reports/index.html`. The public demo is sometimes slow or gets reset. Run `mvn test` again before treating a timeout as a script bug.
