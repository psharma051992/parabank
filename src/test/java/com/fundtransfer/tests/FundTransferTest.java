package com.fundtransfer.tests;

import com.fundtransfer.pages.AccountsPage;
import com.fundtransfer.pages.ActivityPage;
import com.fundtransfer.pages.OpenAccountPage;
import com.fundtransfer.pages.RegisterPage;
import com.fundtransfer.pages.TransferPage;
import com.fundtransfer.support.Customer;
import com.fundtransfer.support.Money;
import com.fundtransfer.support.RegistrationExcel;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class FundTransferTest {

    private static final Logger LOG = Logger.getLogger(FundTransferTest.class.getName());
    private static final int TRANSFER_AMOUNT = 50;
    private static final Path ASSERTION_REPORT = Path.of("target", "surefire-reports", "assertions.html");

    private final List<String[]> checks = new ArrayList<>();
    private WebDriver driver;

    @BeforeMethod
    public void openBrowser() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1400,900");
        if (Boolean.parseBoolean(System.getProperty("headless", "false"))) {
            options.addArguments("--headless=new");
        }
        driver = new ChromeDriver(options);
        // Explicit waits only. An implicit wait on top of WebDriverWait makes timeouts stack.
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(40));
    }

    @AfterMethod(alwaysRun = true)
    public void closeBrowser() {
        writeAssertionReport();
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void newCustomerTransfersFundsBetweenTheirAccounts() {
        Customer customer = RegistrationExcel.nextCustomer();
        String username = customer.username;
        Reporter.log("registration data: " + customer.firstName + " " + customer.lastName
                + " | " + username, true);

        RegisterPage registerPage = new RegisterPage(driver);
        AccountsPage accountsPage = new AccountsPage(driver);
        OpenAccountPage openAccountPage = new OpenAccountPage(driver);
        TransferPage transferPage = new TransferPage(driver);
        ActivityPage activityPage = new ActivityPage(driver);

        registerPage.open();
        check("register form should be open", registerPage.formIsOpen());

        typeAndCheck(registerPage, "[id='customer.firstName']", customer.firstName);
        typeAndCheck(registerPage, "[id='customer.lastName']", customer.lastName);
        typeAndCheck(registerPage, "[id='customer.address.street']", customer.street);
        typeAndCheck(registerPage, "[id='customer.address.city']", customer.city);
        typeAndCheck(registerPage, "[id='customer.address.state']", customer.state);
        typeAndCheck(registerPage, "[id='customer.address.zipCode']", customer.zipCode);
        typeAndCheck(registerPage, "[id='customer.phoneNumber']", customer.phone);
        typeAndCheck(registerPage, "[id='customer.ssn']", customer.ssn);
        typeAndCheck(registerPage, "[id='customer.username']", username);
        typeAndCheck(registerPage, "[id='customer.password']", customer.password);
        typeAndCheck(registerPage, "#repeatedPassword", customer.password);

        registerPage.submit();
        if (registerPage.usernameTaken()) {
            username = RegistrationExcel.freshUsername("user");
            Reporter.log("username was already used, retrying as " + username, true);
            typeAndCheck(registerPage, "[id='customer.username']", username);
            typeAndCheck(registerPage, "[id='customer.password']", customer.password);
            typeAndCheck(registerPage, "#repeatedPassword", customer.password);
            registerPage.submit();
        }
        same("welcome heading", registerPage.welcomeHeading(), "Welcome " + username);
        String success = "Your account was created successfully. You are now logged in.";
        check("registration should leave the customer logged in", registerPage.panelText().contains(success));

        accountsPage.open();
        List<String> accountsAfterRegister = accountsPage.accountIds();
        same("a new customer gets one checking account", accountsAfterRegister.size(), 1);
        String checkingId = accountsAfterRegister.get(0);

        openAccountPage.open();
        check("open-account form should be open", openAccountPage.formIsOpen());
        openAccountPage.chooseSavingsFundedFrom(checkingId);
        same("account type", openAccountPage.selectedType(), "SAVINGS");
        same("funded from account", openAccountPage.selectedFromAccount(), checkingId);
        openAccountPage.submit();
        String savingsId = openAccountPage.newAccountId();
        check("new account id should be shown", !savingsId.isEmpty());
        different("new savings account is not the checking account", savingsId, checkingId);
        same("open account heading", openAccountPage.resultHeading(), "Account Opened!");

        accountsPage.open();
        List<String> accountsBeforeTransfer = accountsPage.accountIds();
        check("checking account should still be listed", accountsBeforeTransfer.contains(checkingId));
        check("new savings account should be listed", accountsBeforeTransfer.contains(savingsId));
        double checkingBefore = accountsPage.balanceOf(checkingId);
        double savingsBefore = accountsPage.balanceOf(savingsId);
        logBalances("before", checkingId, checkingBefore, savingsId, savingsBefore);
        check("checking account should cover the transfer", checkingBefore > TRANSFER_AMOUNT);
        check("new savings account should have a balance before the transfer", savingsBefore > 0);

        transferPage.open();
        check("transfer form should be open", transferPage.formIsOpen());
        transferPage.enterAmount(TRANSFER_AMOUNT);
        same("transfer amount", transferPage.amountField(), Integer.toString(TRANSFER_AMOUNT));
        transferPage.chooseAccounts(checkingId, savingsId);
        same("transfer from account", transferPage.selectedFromAccount(), checkingId);
        same("transfer to account", transferPage.selectedToAccount(), savingsId);
        transferPage.submit();
        same("transfer confirmation amount", transferPage.amountResult(), Money.format(TRANSFER_AMOUNT));
        same("transfer confirmation from account", transferPage.fromAccountResult(), checkingId);
        same("transfer confirmation to account", transferPage.toAccountResult(), savingsId);

        accountsPage.open();
        List<String> accountsAfterTransfer = accountsPage.accountIds();
        check("checking account is listed after the transfer", accountsAfterTransfer.contains(checkingId));
        check("savings account is listed after the transfer", accountsAfterTransfer.contains(savingsId));
        double checkingAfter = accountsPage.balanceOf(checkingId);
        double savingsAfter = accountsPage.balanceOf(savingsId);
        logBalances("after", checkingId, checkingAfter, savingsId, savingsAfter);
        same("checking balance decreased by the transfer",
                Money.toCents(checkingAfter), Money.toCents(checkingBefore - TRANSFER_AMOUNT));
        same("savings balance increased by the transfer",
                Money.toCents(savingsAfter), Money.toCents(savingsBefore + TRANSFER_AMOUNT));

        // Opening the savings account already posts "Funds Transfer Sent/Received"
        // for the minimum deposit, so the amount has to match as well.
        String amountText = Money.format(TRANSFER_AMOUNT);
        activityPage.open(checkingId);
        same("checking activity account", activityPage.accountIdShown(), checkingId);
        assertTransaction(activityPage.rows(), "Funds Transfer Sent", amountText, 1);
        activityPage.open(savingsId);
        same("savings activity account", activityPage.accountIdShown(), savingsId);
        assertTransaction(activityPage.rows(), "Funds Transfer Received", amountText, 2);
    }

    private void typeAndCheck(RegisterPage page, String css, String value) {
        page.type(css, value);
        same("typed " + css, page.valueOf(css), value);
    }

    private void same(String step, Object actual, Object expected) {
        note(String.valueOf(expected).equals(String.valueOf(actual)), step, String.valueOf(expected), String.valueOf(actual));
        Assert.assertEquals(actual, expected, step);
    }

    private void different(String step, Object actual, Object other) {
        note(!String.valueOf(other).equals(String.valueOf(actual)), step, "not " + other, String.valueOf(actual));
        Assert.assertNotEquals(actual, other, step);
    }

    private void check(String step, boolean condition) {
        note(condition, step, "true", String.valueOf(condition));
        Assert.assertTrue(condition, step);
    }

    private void note(boolean passed, String step, String expected, String actual) {
        String result = passed ? "PASS" : "FAIL";
        checks.add(new String[] {result, step, expected, actual});
        Reporter.log(result + " | " + step + " | expected [" + expected + "] actual [" + actual + "]", true);
    }

    private void writeAssertionReport() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Assertions</title></head><body>");
        html.append("<h1>Assertions</h1><table border=\"1\" cellpadding=\"6\">");
        html.append("<tr><th>Result</th><th>Check</th><th>Expected</th><th>Actual</th></tr>");
        for (String[] row : checks) {
            String color = "PASS".equals(row[0]) ? "#d9f2d9" : "#f8d0d0";
            html.append("<tr style=\"background:").append(color).append("\">");
            for (String cell : row) {
                html.append("<td>").append(escape(cell)).append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</table></body></html>");
        try {
            Files.createDirectories(ASSERTION_REPORT.getParent());
            Files.writeString(ASSERTION_REPORT, html.toString(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LOG.warning("could not write assertion report: " + ex.getMessage());
        }
    }

    private String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void logBalances(String when, String checkingId, double checking, String savingsId, double savings) {
        String line = when + " | checking " + checkingId + ": " + checking + " | savings " + savingsId + ": " + savings;
        LOG.info(line);
        Reporter.log(line, true);
    }

    /** columns: 0 description, 1 debit, 2 credit */
    private void assertTransaction(List<String[]> rows, String description, String amountText, int amountColumn) {
        check("transaction table should have rows", !rows.isEmpty());
        List<String> seen = new ArrayList<>();
        boolean matched = false;
        for (String[] row : rows) {
            seen.add(row[0] + " " + row[amountColumn]);
            if (row[0].contains(description) && amountText.equals(row[amountColumn])) {
                matched = true;
            }
        }
        note(matched, description + " " + amountText, amountText, matched ? amountText : String.join(" | ", seen));
        Assert.assertTrue(matched,
                "missing \"" + description + "\" " + amountText + ". Saw: " + String.join(" | ", seen));
    }
}
