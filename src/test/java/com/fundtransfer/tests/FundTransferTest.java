package com.fundtransfer.tests;

import com.fundtransfer.pages.AccountsPage;
import com.fundtransfer.pages.ActivityPage;
import com.fundtransfer.pages.OpenAccountPage;
import com.fundtransfer.pages.RegisterPage;
import com.fundtransfer.pages.TransferPage;
import com.fundtransfer.support.Customer;
import com.fundtransfer.support.Money;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class FundTransferTest {

    private static final Logger LOG = Logger.getLogger(FundTransferTest.class.getName());
    private static final int TRANSFER_AMOUNT = 50;

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
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void newCustomerTransfersFundsBetweenTheirAccounts() {
        String username = "alex" + System.currentTimeMillis();
        Customer customer = new Customer(
                "Alex", "Turner", "12 Oak Street", "Austin", "TX", "78701",
                "5125550148", "123-45-6789", username, "pass123");

        RegisterPage registerPage = new RegisterPage(driver);
        AccountsPage accountsPage = new AccountsPage(driver);
        OpenAccountPage openAccountPage = new OpenAccountPage(driver);
        TransferPage transferPage = new TransferPage(driver);
        ActivityPage activityPage = new ActivityPage(driver);

        registerPage.open();
        registerPage.register(customer);
        Assert.assertEquals(registerPage.welcomeHeading(), "Welcome " + username);
        Assert.assertTrue(
                registerPage.panelText().contains("Your account was created successfully. You are now logged in."),
                "registration should leave the customer logged in");

        accountsPage.open();
        List<String> accountsAfterRegister = accountsPage.accountIds();
        Assert.assertEquals(accountsAfterRegister.size(), 1, "a new customer gets one checking account");
        String checkingId = accountsAfterRegister.get(0);

        String savingsId = openAccountPage.openSavingsFundedFrom(checkingId);
        Assert.assertNotEquals(savingsId, checkingId);
        Assert.assertEquals(openAccountPage.resultHeading(), "Account Opened!");

        accountsPage.open();
        double checkingBefore = accountsPage.balanceOf(checkingId);
        double savingsBefore = accountsPage.balanceOf(savingsId);
        logBalances("before", checkingId, checkingBefore, savingsId, savingsBefore);
        Assert.assertTrue(checkingBefore > TRANSFER_AMOUNT, "checking account should cover the transfer");

        transferPage.send(TRANSFER_AMOUNT, checkingId, savingsId);
        Assert.assertEquals(transferPage.amountResult(), Money.format(TRANSFER_AMOUNT));
        Assert.assertEquals(transferPage.fromAccountResult(), checkingId);
        Assert.assertEquals(transferPage.toAccountResult(), savingsId);

        accountsPage.open();
        double checkingAfter = accountsPage.balanceOf(checkingId);
        double savingsAfter = accountsPage.balanceOf(savingsId);
        logBalances("after", checkingId, checkingAfter, savingsId, savingsAfter);

        // Opening the savings account already posts "Funds Transfer Sent/Received"
        // for the minimum deposit, so the amount has to match as well.
        String amountText = Money.format(TRANSFER_AMOUNT);
        activityPage.open(checkingId);
        assertTransaction(activityPage.rows(), "Funds Transfer Sent", amountText, 1);
        activityPage.open(savingsId);
        assertTransaction(activityPage.rows(), "Funds Transfer Received", amountText, 2);

        Assert.assertEquals(Money.toCents(checkingAfter), Money.toCents(checkingBefore - TRANSFER_AMOUNT));
        Assert.assertEquals(Money.toCents(savingsAfter), Money.toCents(savingsBefore + TRANSFER_AMOUNT));
    }

    private void logBalances(String when, String checkingId, double checking, String savingsId, double savings) {
        String line = when + " | checking " + checkingId + ": " + checking + " | savings " + savingsId + ": " + savings;
        LOG.info(line);
        Reporter.log(line, true);
    }

    /** columns: 0 description, 1 debit, 2 credit */
    private void assertTransaction(List<String[]> rows, String description, String amountText, int amountColumn) {
        Assert.assertFalse(rows.isEmpty(), "transaction table should have rows");
        List<String> seen = new ArrayList<>();
        boolean matched = false;
        for (String[] row : rows) {
            seen.add(row[0] + " " + row[amountColumn]);
            if (row[0].contains(description) && amountText.equals(row[amountColumn])) {
                matched = true;
            }
        }
        Assert.assertTrue(matched,
                "missing \"" + description + "\" " + amountText + ". Saw: " + String.join(" | ", seen));
    }
}
