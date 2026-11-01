package com.fundtransfer.pages;

import com.fundtransfer.support.Customer;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
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

    public void register(Customer customer) {
        type("[id='customer.firstName']", customer.firstName);
        type("[id='customer.lastName']", customer.lastName);
        type("[id='customer.address.street']", customer.street);
        type("[id='customer.address.city']", customer.city);
        type("[id='customer.address.state']", customer.state);
        type("[id='customer.address.zipCode']", customer.zipCode);
        type("[id='customer.phoneNumber']", customer.phone);
        type("[id='customer.ssn']", customer.ssn);
        type("[id='customer.username']", customer.username);
        type("[id='customer.password']", customer.password);
        type("#repeatedPassword", customer.password);
        driver.findElement(By.cssSelector("input[value='Register']")).click();
    }

    public String welcomeHeading() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#rightPanel h1.title")))
                .getText()
                .trim();
    }

    public String panelText() {
        return driver.findElement(By.id("rightPanel")).getText();
    }

    private void type(String css, String value) {
        driver.findElement(By.cssSelector(css)).sendKeys(value);
    }
}
