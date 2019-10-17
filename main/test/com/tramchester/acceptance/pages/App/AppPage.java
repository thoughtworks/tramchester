package com.tramchester.acceptance.pages.App;

import com.tramchester.acceptance.pages.Page;
import com.tramchester.acceptance.pages.ProvidesDateInput;
import com.tramchester.integration.Stations;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AppPage extends Page {
    public static final String DATE = "date";
    private final ProvidesDateInput providesDateInput;
    private long timeoutInSeconds = 2;

    private String FROM_STOP = "fromStop";
    private String TO_STOP = "toStop";
    private String TIME = "time";
    private String RESULTS = "results";

    public AppPage(WebDriver driver, ProvidesDateInput providesDateInput) {
        super(driver);
        this.providesDateInput = providesDateInput;
    }

    public void load(String url) {
        driver.get(url);
    }

    public void waitForToStops() {
        WebDriverWait wait = createWait();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id(FROM_STOP)));
        WebElement fromStopElement = findElementById(FROM_STOP);
        wait.until(ExpectedConditions.textToBePresentInElement(fromStopElement, Stations.Altrincham.getName()));
    }

    public boolean hasConsentButton() {
        try {
            findElementById("consent");
            return true;
        }
        catch (NoSuchElementException ex) {
            return false;
        }
    }

    public void planAJourney() {
        findElementById("plan").click();
    }

    public void consent() {
        WebElement button = findElementById("consent");
        button.click();
    }

    public void setStart(String start) {
        setSelector(start, FROM_STOP);
    }

    public void setDest(String destination) {
        setSelector(destination, TO_STOP);
    }

    private void setSelector(String start, String id) {
        Select selector = new Select(driver.findElement(By.id(id)));
        selector.selectByVisibleText(start);
    }

    // TODO
    public void setDate(LocalDate localDate) {
        WebElement element = getDateElement();

        String input = providesDateInput.createDateInput(localDate);

        element.click();
        element.sendKeys(Keys.ARROW_LEFT);
        element.sendKeys(input);
    }

    public void setTime(LocalTime time) {
        WebElement element = getTimeElement();

        Actions builder  = new Actions(driver);
        String input = providesDateInput.createTimeFormat(time);
        int chars = input.length();

        while (chars-- > 0) {
            builder.sendKeys(element, Keys.ARROW_LEFT);
        }
        builder.sendKeys(element, input);
        builder.pause(Duration.ofMillis(50));
        builder.build().perform();
    }

    private WebElement getTimeElement() {
        waitForElement(TIME, timeoutInSeconds);
        return findElementById(TIME);
    }

    private WebDriverWait createWait() {
        return new WebDriverWait(driver, timeoutInSeconds);
    }

    private WebElement getDateElement() {
        waitForElement(DATE, timeoutInSeconds);
        return findElementById(DATE);
    }

    public String getFromStop() {
        Select selector = new Select(driver.findElement(By.id(FROM_STOP)));
        return selector.getFirstSelectedOption().getText();
    }

    public String getToStop() {
        Select selector = new Select(driver.findElement(By.id(TO_STOP)));
        return selector.getFirstSelectedOption().getText();
    }

    public String getTime() {
        return getTimeElement().getAttribute("value");
    }

    public LocalDate getDate() {
        String rawDate = getDateElement().getAttribute("value");
        return LocalDate.parse(rawDate);
    }

    public boolean resultsClickable() {
        try {
            By locateResults = By.id((RESULTS));

            waitForClickable(locateResults);
            return true;
        }
        catch (TimeoutException exception) {
            return false;
        }
    }

    private void waitForClickable(By locator) {
        waitForClickableLocator(locator);
    }

    public List<SummaryResult> getResults() {
        List<SummaryResult> results = new ArrayList<>();
        By findResults = By.id(RESULTS);
        WebElement resultsDiv = driver.findElement(findResults);

        WebElement tableBody = resultsDiv.findElement(By.tagName("tbody"));
        List<WebElement> rows = tableBody.findElements(By.className("journeySummary"));
        rows.forEach(row -> results.add(new SummaryResult(row, tableBody)));

        return results;
    }

    public List<String> getRecentFromStops() {
        By fromGroupRecent = By.id("fromGroupRecent");
        waitForClickableLocator(fromGroupRecent);
        WebElement recentElement = driver.findElement(fromGroupRecent);

        return getStopNames(recentElement);
    }

    public List<String> getAllStopsFromStops() {
        By fromGroupRecent = By.id("fromGroupAll Stops");

        waitForClickableLocator(fromGroupRecent);
        WebElement recentElement = driver.findElement(fromGroupRecent);

        return getStopNames(recentElement);
    }

    public List<String> getToStops() {
        By toStops = By.id(TO_STOP);
        waitForClickable(toStops);

        WebElement elements = driver.findElement(toStops);
        return getStopNames(elements);
    }

    private List<String> getStopNames(WebElement recentElement) {
        List<WebElement> stopElements = recentElement.findElements(By.className("stop"));
        return stopElements.stream().map(element -> element.getText()).collect(Collectors.toList());
    }

    public boolean noResults() {
        try {
            waitForElement("noResults",timeoutInSeconds);
            return true;
        }
        catch (TimeoutException notFound) {
            return false;
        }
    }

    public void waitForClickable(WebElement element) {
        createWait().until(ExpectedConditions.elementToBeClickable(element));
    }

    private void waitForClickableLocator(By fromGroupRecent) {
        createWait().until(ExpectedConditions.elementToBeClickable(fromGroupRecent));
    }

    public boolean notesPresent() {
        return waitForCondition(ExpectedConditions.presenceOfElementLocated(By.id("NotesList")));
    }

    public String getBuild() {
        return waitForAndGet(By.id("buildNumber"));
    }

    public String getValidFrom() {
        return waitForAndGet(By.id("validFrom"));
    }

    public String getValidUntil() {
        return waitForAndGet(By.id("validUntil"));
    }

    private String waitForAndGet(By locator) {
        createWait().until(ExpectedConditions.presenceOfElementLocated(locator));
        return driver.findElement(locator).getText();
    }

    public void displayDisclaimer() {
        WebElement button = createWait().until(ExpectedConditions.presenceOfElementLocated(By.id("disclaimerButton")));
        button.click();
    }

    public boolean waitForDisclaimerVisible() {
        return waitForCondition(ExpectedConditions.visibilityOfElementLocated(By.id("modal-disclaimer")));
    }

    public boolean waitForDisclaimerInvisible() {
        return waitForCondition(ExpectedConditions.invisibilityOfElementLocated(By.id("modal-disclaimer")));
    }

    public void dismissDisclaimer() {
        WebElement diag = driver.findElement(By.id("modal-disclaimer"));
        WebElement button = diag.findElement(By.tagName("button"));
        button.click();
    }

    private boolean waitForCondition(ExpectedCondition<?> expectedCondition) {
        try {
            createWait().until(expectedCondition);
            return true;
        } catch (TimeoutException notShown) {
            return false;
        }
    }

}
