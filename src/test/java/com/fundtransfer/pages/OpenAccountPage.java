package com.fundtransfer.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class OpenAccountPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public OpenAccountPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(60));
    }

    public String openSavingsFundedFrom(String fromAccountId) {
        driver.findElement(By.linkText("Open New Account")).click();
        // The from-account dropdown is empty until the customer's accounts come back.
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector("#fromAccountId option"), 0));

        new Select(driver.findElement(By.id("type"))).selectByVisibleText("SAVINGS");
        new Select(driver.findElement(By.id("fromAccountId"))).selectByValue(fromAccountId);
        driver.findElement(By.cssSelector("input[value='Open New Account']")).click();

        WebElement newAccount = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("newAccountId")));
        wait.until(driver -> !newAccount.getText().trim().isEmpty());
        return newAccount.getText().trim();
    }

    public String resultHeading() {
        return driver.findElement(By.cssSelector("#openAccountResult h1.title")).getText().trim();
    }
}
