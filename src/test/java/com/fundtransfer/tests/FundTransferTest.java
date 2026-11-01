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
        Assert.assertTrue(registerPage.formIsOpen(), "register form should be open");

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
        Assert.assertEquals(registerPage.welcomeHeading(), "Welcome " + username);
        Assert.assertTrue(
                registerPage.panelText().contains("Your account was created successfully. You are now logged in."),
                "registration should leave the customer logged in");

        accountsPage.open();
        List<String> accountsAfterRegister = accountsPage.accountIds();
        Assert.assertEquals(accountsAfterRegister.size(), 1, "a new customer gets one checking account");
        String checkingId = accountsAfterRegister.get(0);

        openAccountPage.open();
        Assert.assertTrue(openAccountPage.formIsOpen(), "open-account form should be open");
        openAccountPage.chooseSavingsFundedFrom(checkingId);
        Assert.assertEquals(openAccountPage.selectedType(), "SAVINGS");
        Assert.assertEquals(openAccountPage.selectedFromAccount(), checkingId);
        openAccountPage.submit();
        String savingsId = openAccountPage.newAccountId();
        Assert.assertNotEquals(savingsId, checkingId);
        Assert.assertEquals(openAccountPage.resultHeading(), "Account Opened!");

        accountsPage.open();
        List<String> accountsBeforeTransfer = accountsPage.accountIds();
        Assert.assertTrue(accountsBeforeTransfer.contains(checkingId), "checking account should still be listed");
        Assert.assertTrue(accountsBeforeTransfer.contains(savingsId), "new savings account should be listed");
        double checkingBefore = accountsPage.balanceOf(checkingId);
        double savingsBefore = accountsPage.balanceOf(savingsId);
        logBalances("before", checkingId, checkingBefore, savingsId, savingsBefore);
        Assert.assertTrue(checkingBefore > TRANSFER_AMOUNT, "checking account should cover the transfer");

        transferPage.open();
        Assert.assertTrue(transferPage.formIsOpen(), "transfer form should be open");
        transferPage.enterAmount(TRANSFER_AMOUNT);
        Assert.assertEquals(transferPage.amountField(), Integer.toString(TRANSFER_AMOUNT));
        transferPage.chooseAccounts(checkingId, savingsId);
        Assert.assertEquals(transferPage.selectedFromAccount(), checkingId);
        Assert.assertEquals(transferPage.selectedToAccount(), savingsId);
        transferPage.submit();
        Assert.assertEquals(transferPage.amountResult(), Money.format(TRANSFER_AMOUNT));
        Assert.assertEquals(transferPage.fromAccountResult(), checkingId);
        Assert.assertEquals(transferPage.toAccountResult(), savingsId);

        accountsPage.open();
        List<String> accountsAfterTransfer = accountsPage.accountIds();
        Assert.assertTrue(accountsAfterTransfer.contains(checkingId));
        Assert.assertTrue(accountsAfterTransfer.contains(savingsId));
        double checkingAfter = accountsPage.balanceOf(checkingId);
        double savingsAfter = accountsPage.balanceOf(savingsId);
        logBalances("after", checkingId, checkingAfter, savingsId, savingsAfter);
        Assert.assertEquals(Money.toCents(checkingAfter), Money.toCents(checkingBefore - TRANSFER_AMOUNT));
        Assert.assertEquals(Money.toCents(savingsAfter), Money.toCents(savingsBefore + TRANSFER_AMOUNT));

        // Opening the savings account already posts "Funds Transfer Sent/Received"
        // for the minimum deposit, so the amount has to match as well.
        String amountText = Money.format(TRANSFER_AMOUNT);
        activityPage.open(checkingId);
        assertTransaction(activityPage.rows(), "Funds Transfer Sent", amountText, 1);
        activityPage.open(savingsId);
        assertTransaction(activityPage.rows(), "Funds Transfer Received", amountText, 2);
    }

    private void typeAndCheck(RegisterPage page, String css, String value) {
        page.type(css, value);
        Assert.assertEquals(page.valueOf(css), value);
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
