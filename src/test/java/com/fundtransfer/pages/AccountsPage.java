package com.fundtransfer.pages;

import com.fundtransfer.support.Money;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class AccountsPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public AccountsPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(60));
    }

    public void open() {
        driver.findElement(By.linkText("Accounts Overview")).click();
        // The table shell is in the HTML. The account rows arrive from a later ajax call.
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#accountTable tbody tr a")));
    }

    public List<String> accountIds() {
        List<String> ids = new ArrayList<>();
        for (WebElement link : driver.findElements(By.cssSelector("#accountTable tbody tr a"))) {
            ids.add(link.getText().trim());
        }
        return ids;
    }

    public double balanceOf(String accountId) {
        WebElement row = driver.findElement(By.xpath(
                "//table[@id='accountTable']//tr[td/a[normalize-space()='" + accountId + "']]"));
        String balanceText = row.findElements(By.tagName("td")).get(1).getText();
        return Money.parse(balanceText);
    }
}
