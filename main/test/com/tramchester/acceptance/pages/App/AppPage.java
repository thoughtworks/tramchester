package com.tramchester.acceptance.pages.App;

import com.tramchester.acceptance.pages.Page;
import com.tramchester.acceptance.pages.ProvidesDateInput;
import com.tramchester.domain.presentation.LatLong;
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
    private final ProvidesDateInput providesDateInput;
    private long timeoutInSeconds = 5;

//    public static LatLong NearAltrincham = new LatLong(53.394982299999995D,-2.3581502D);
//    public static LatLong NearPiccGardens = new LatLong(53.480972D,-2.2380073D);

    public static final String DATE = "date";
    private String FROM_STOP = "fromStop";
    private String TO_STOP = "toStop";
    private String TIME = "time";
    private String RESULTS = "results";
    private By disclaimer;
    private By cookieDialogue;

    public AppPage(WebDriver driver, ProvidesDateInput providesDateInput) {
        super(driver);
        this.providesDateInput = providesDateInput;
        disclaimer = By.id("modal-disclaimer");
        cookieDialogue = By.id("modal-cookieConsent");
    }

    public void load(String url) {
        driver.get(url);
    }

    public void waitForToStops() {
        WebDriverWait wait = createWait();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id(FROM_STOP)));
        WebElement fromStopElement = findElementById(FROM_STOP);
        wait.until(ExpectedConditions.textToBePresentInElement(fromStopElement, Stations.ManAirport.getName()));
    }

    public void planAJourney() {
        findPlanButton().click();
    }


    public void earlier() {
        findElementById("earlierButton").click();
    }

    public void later() {
        findElementById("laterButton").click();
    }

    public boolean searchEnabled() {
        return findPlanButton().isEnabled();
    }

    private WebElement findPlanButton() {
        return findElementById("plan");
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
        return selector.getFirstSelectedOption().getText().trim();
    }

    public String getToStop() {
        Select selector = new Select(driver.findElement(By.id(TO_STOP)));
        return selector.getFirstSelectedOption().getText().trim();
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
        try {
            return getStopsByGroupName("fromGroupRecent");
        }
        catch (TimeoutException notFound) {
            return new ArrayList<>();
        }
    }

    public List<String> getRecentToStops() {
        try {
            return getStopsByGroupName("toGroupRecent");
        }
        catch (TimeoutException notFound) {
            return new ArrayList<>();
        }
    }

    public List<String> getAllStopsFromStops() {
        return getStopsByGroupName("fromGroupAll Stops");
    }

    public List<String> getAllStopsToStops() {
        return getStopsByGroupName("toGroupAll Stops");
    }

    public List<String> getNearestFromStops() {
        return getStopsByGroupName("fromGroupNearest Stops");
    }

    public List<String> getNearbyToStops() {
        try {
            return getStopsByGroupName("toGroupNearby");
        }
        catch (TimeoutException notFound) {
            return new ArrayList<>();
        }
    }

    public List<String> getNearbyFromStops() {
        try {
            return getStopsByGroupName("fromGroupNearby");
        }
        catch (TimeoutException notFound) {
            return new ArrayList<>();
        }
    }

    public List<String> getToStops() {
        By toStops = By.id(TO_STOP);
        waitForClickable(toStops);

        WebElement elements = driver.findElement(toStops);
        return getStopNames(elements);
    }

    private List<String> getStopNames(WebElement groupElement) {
        List<WebElement> stopElements = groupElement.findElements(By.className("stop"));
        List<String> results = stopElements.stream().map(WebElement::getText).map(String::trim).collect(Collectors.toList());
        return results;
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
        return waitForVisability(disclaimer);
    }

    public boolean waitForDisclaimerInvisible() {
        return waitForInvisability(disclaimer);
    }


    public boolean waitForCookieAgreementInvisible() {
        return waitForInvisability(cookieDialogue);
    }

    public boolean waitForCookieAgreementVisible() {
        return waitForVisability(cookieDialogue);
    }

    public void dismissDisclaimer() {
        okToModal(disclaimer);
    }

    public void agreeToCookies() {
        okToModal(cookieDialogue);
    }

    private boolean waitForCondition(ExpectedCondition<?> expectedCondition) {
        try {
            createWait().until(expectedCondition);
            return true;
        } catch (TimeoutException notShown) {
            return false;
        }
    }

    private boolean waitForVisability(By locator) {
        return waitForCondition(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    private boolean waitForInvisability(By locator) {
        return waitForCondition(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    private void okToModal(By locator) {
        WebElement diag = driver.findElement(locator);
        WebElement button = diag.findElement(By.tagName("button"));
        button.click();
    }

    public void selectNow() {
        WebElement nowButton = driver.findElement(By.id("nowButton"));
        nowButton.click();
    }

    public void selectToday() {
        WebElement todayButton = driver.findElement(By.id("todayButton"));
        todayButton.click();
    }

    private List<String> getStopsByGroupName(String groupName) {
        By fromGroup = By.id(groupName);
        waitForClickableLocator(fromGroup);
        WebElement groupElement = driver.findElement(fromGroup);
        return getStopNames(groupElement);
    }

    public boolean hasLocation() {
        return "true".equals(findElementById("havePos").getText());
    }

    public void waitForReady() {
        // geo loc on firefox can be slow even when stubbing location via a file....
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.elementToBeClickable(By.id("plan")));
    }

}
