package com.fundtransfer.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ActivityPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public ActivityPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(60));
    }

    public void open(String accountId) {
        driver.get("https://parabank.parasoft.com/parabank/activity.htm?id=" + accountId);
        wait.until(ExpectedConditions.textToBe(By.id("accountId"), accountId));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#transactionTable tbody tr")));
    }

    public List<String[]> rows() {
        List<String[]> rows = new ArrayList<>();
        for (WebElement row : driver.findElements(By.cssSelector("#transactionTable tbody tr"))) {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            String description = cells.get(1).getText().replaceAll("\\s+", " ").trim();
            String debit = cells.get(2).getText().trim();
            String credit = cells.get(3).getText().trim();
            rows.add(new String[] {description, debit, credit});
        }
        return rows;
    }
}
