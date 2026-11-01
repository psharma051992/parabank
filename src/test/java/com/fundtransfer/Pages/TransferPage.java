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

    public void send(int amount, String fromAccountId, String toAccountId) {
        driver.findElement(By.linkText("Transfer Funds")).click();
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector("#fromAccountId option"), 0));

        driver.findElement(By.id("amount")).sendKeys(Integer.toString(amount));
        new Select(driver.findElement(By.id("fromAccountId"))).selectByValue(fromAccountId);
        new Select(driver.findElement(By.id("toAccountId"))).selectByValue(toAccountId);
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
