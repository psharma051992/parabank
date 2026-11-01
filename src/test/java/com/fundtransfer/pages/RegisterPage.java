package com.fundtransfer.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class RegisterPage {

    private static final String URL = "https://parabank.parasoft.com/parabank/register.htm";

    private final WebDriver driver;
    private final WebDriverWait wait;

    public RegisterPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(60));
    }

    public void open() {
        driver.get(URL);
    }

    public boolean formIsOpen() {
        return driver.findElement(By.cssSelector("[id='customer.firstName']")).isDisplayed();
    }

    public void type(String css, String value) {
        WebElement field = driver.findElement(By.cssSelector(css));
        field.clear();
        field.sendKeys(value);
    }

    public String valueOf(String css) {
        return driver.findElement(By.cssSelector(css)).getAttribute("value");
    }

    public void submit() {
        driver.findElement(By.cssSelector("input[value='Register']")).click();
    }

    public boolean usernameTaken() {
        By taken = By.cssSelector("[id='customer.username.errors']");
        By heading = By.cssSelector("#rightPanel h1.title");
        wait.until(driver -> !driver.findElements(taken).isEmpty() || headingSaysWelcome(heading));
        return !driver.findElements(taken).isEmpty();
    }

    private boolean headingSaysWelcome(By heading) {
        for (WebElement element : driver.findElements(heading)) {
            if (element.getText().contains("Welcome")) {
                return true;
            }
        }
        return false;
    }

    public String welcomeHeading() {
        // The signup page already has this heading, and its text is "Signing up is easy!".
        // Wait until registration replaces that text with "Welcome {username}".
        By heading = By.cssSelector("#rightPanel h1.title");
        wait.until(ExpectedConditions.textToBePresentInElementLocated(heading, "Welcome"));
        return driver.findElement(heading).getText().trim();
    }

    public String panelText() {
        return driver.findElement(By.id("rightPanel")).getText();
    }
}
