package com.fundtransfer.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class TransferPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public TransferPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(60));
    }

    public void open() {
        driver.findElement(By.linkText("Transfer Funds")).click();
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector("#fromAccountId option"), 0));
    }

    public boolean formIsOpen() {
        return driver.findElement(By.id("amount")).isDisplayed();
    }

    public void enterAmount(int amount) {
        driver.findElement(By.id("amount")).sendKeys(Integer.toString(amount));
    }

    public String amountField() {
        return driver.findElement(By.id("amount")).getAttribute("value").trim();
    }

    public void chooseAccounts(String fromAccountId, String toAccountId) {
        new Select(driver.findElement(By.id("fromAccountId"))).selectByValue(fromAccountId);
        new Select(driver.findElement(By.id("toAccountId"))).selectByValue(toAccountId);
    }

    public String selectedFromAccount() {
        return new Select(driver.findElement(By.id("fromAccountId"))).getFirstSelectedOption().getAttribute("value").trim();
    }

    public String selectedToAccount() {
        return new Select(driver.findElement(By.id("toAccountId"))).getFirstSelectedOption().getAttribute("value").trim();
    }

    public void submit() {
        driver.findElement(By.cssSelector("#transferForm input[type='submit']")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h1[normalize-space()='Transfer Complete!']")));
    }

    public String amountResult() {
        return driver.findElement(By.id("amountResult")).getText().trim();
    }

    public String fromAccountResult() {
        return driver.findElement(By.id("fromAccountIdResult")).getText().trim();
    }

    public String toAccountResult() {
        return driver.findElement(By.id("toAccountIdResult")).getText().trim();
    }
}
