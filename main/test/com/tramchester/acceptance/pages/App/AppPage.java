package com.tramchester.acceptance.pages.App;

import com.tramchester.acceptance.pages.Page;
import com.tramchester.acceptance.pages.ProvidesDateInput;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.openqa.selenium.support.ui.ExpectedConditions.*;

public class AppPage extends Page {
    private static final String MODAL_COOKIE_CONSENT = "modal-cookieConsent";
    private static final String MODAL_DISCLAIMER = "modal-disclaimer";
    private static final String PLAN = "plan";

    private static final String START_GROUP = "startGroup";
    private static final String DESTINATION_GROUP = "destinationGroup";

    private final ProvidesDateInput providesDateInput;
    private final long timeoutInSeconds = 15;

    private static final String DATE_OUTPUT = "hiddendate";
    public static final String FROM_STOP = "startStop";
    private static final String TO_STOP = "destinationStop";
    private static final String TIME = "time";
    private static final String RESULTS = "results";

    public AppPage(WebDriver driver, ProvidesDateInput providesDateInput) {
        super(driver);
        this.providesDateInput = providesDateInput;
    }

    public void load(String url) {
        driver.get(url);
    }

    public boolean waitForToStops() {
        //TramStations station = TramStations.ManAirport;
        int expected = TramStations.NumberOf;
        return waitForStops(FROM_STOP) && waitForStops(TO_STOP);
    }

    public boolean waitForStops(String stopId) {
        WebElement fromStopElement = createWait().until(presenceOfElementLocated(By.id(stopId)));

        createWait().until(presenceOfNestedElementLocatedBy(fromStopElement, By.className("stop")));
        //createWait().until(numberOfElementsToBe(By.className("stop"), 10));
        //createWait().until(textToBePresentInElement(fromStopElement, station.getName()));

        return fromStopElement.isEnabled() && fromStopElement.isDisplayed();
    }

    public void planAJourney() {
        findAndClickElement(PLAN);
    }

    private void findAndClickElement(String elementId) {
        WebElement webElement = driver.findElement(By.id(elementId));

        Actions actions = moveToElement(webElement);
        actions.click().perform();
    }

    @NotNull
    private Actions moveToElement(WebElement webElement) {
        // sigh
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView();", webElement);
        Actions actions = new Actions(driver);
        actions.moveToElement(webElement).perform();
        return actions;
    }

    public void earlier() {
        findAndClickElement("earlierButton");
    }

    public void later() {
        findAndClickElement("laterButton");
    }

    public boolean searchEnabled() {
        WebElement planButton = findElementById(PLAN);
        createWait().until(elementToBeClickable(planButton));
        return planButton.isEnabled();
    }

    public void setStart(String start) {
        setSelector(FROM_STOP, start);
    }

    public void setDest(String destination) {
        setSelector(TO_STOP, destination);
    }

    private void setSelector(String id, String selectionName) {
        WebElement element = driver.findElement(By.id(id));
        moveToElement(element);
        createWait().until(elementToBeClickable(element));

        Select selector = new Select(element);
        selector.selectByVisibleText(selectionName);
    }

    public void setSpecificDate(LocalDate targetDate) {
        LocalDate currentDate = TestEnv.LocalNow().toLocalDate();

        WebElement dateElement = findElementById("date");
        moveToElement(dateElement).click().perform();

        WebElement dialog = waitForElement(By.xpath("//div[@aria-roledescription='TravelDateCalendar']"),
                timeoutInSeconds);
        dialog.sendKeys(Keys.HOME); // today

        // forwards or back as needed, very clunky....
        if (targetDate.isAfter(currentDate)) {
            long diffInDays = (targetDate.toEpochDay() - currentDate.toEpochDay());
            for (int i = 0; i < diffInDays; i++) {
                dialog.sendKeys(Keys.RIGHT);
            }
        } else {
            long diffInDays = (currentDate.toEpochDay() - targetDate.toEpochDay());
            for (int i = 0; i < diffInDays; i++) {
                dialog.sendKeys(Keys.LEFT);
            }
        }

        dialog.sendKeys(Keys.ENTER);
    }

    public void setTime(TramTime time) {
        WebElement element = getTimeElement();

        Actions builder  = new Actions(driver);
        String input = providesDateInput.createTimeFormat(time.asLocalTime());
        int chars = input.length();

        moveToElement(element);
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

    private WebElement getDateElementOutput() {
        waitForElement(DATE_OUTPUT, timeoutInSeconds);
        return findElementById(DATE_OUTPUT);
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
        String rawDate = getDateElementOutput().getAttribute("value");
        return LocalDate.parse(rawDate);
    }

    public boolean resultsClickable() {
        try {
            By locateResults = By.id((RESULTS));
            waitForClickableLocator(locateResults);
            return true;
        }
        catch (TimeoutException exception) {
            return false;
        }
    }

    public List<TestResultSummaryRow> getResults() {
        List<TestResultSummaryRow> results = new ArrayList<>();
        By resultsById = By.id(RESULTS);
        WebElement resultsDiv = new WebDriverWait(driver, 10).
                until(elementToBeClickable(resultsById));

        WebElement tableBody = resultsDiv.findElement(By.tagName("tbody"));
        List<WebElement> rows = tableBody.findElements(By.className("journeySummary"));
        rows.forEach(row -> results.add(new TestResultSummaryRow(row, tableBody)));

        return results;
    }

    public List<String> getRecentFromStops() {
        try {
            return getEnabledStopsByGroupName(START_GROUP + "Recent");
        }
        catch (TimeoutException notFound) {
            return new ArrayList<>();
        }
    }

    public List<String> getRecentToStops() {
        try {
            return getEnabledStopsByGroupName(DESTINATION_GROUP + "Recent");
        }
        catch (TimeoutException notFound) {
            return new ArrayList<>();
        }
    }

    public List<String> getAllStopsFromStops() {
        return getEnabledStopsByGroupName(START_GROUP + "AllStops");
    }

    public List<String> getAllStopsToStops() {
        return getEnabledStopsByGroupName(DESTINATION_GROUP + "AllStops");
    }

    public List<String> getNearestFromStops() {
        return getEnabledStopsByGroupName(START_GROUP + "NearestStops");
    }

    public List<String> getNearbyToStops() {
        return getEnabledStopsByGroupName(DESTINATION_GROUP + "Nearby");
    }

    public List<String> getNearbyFromStops() {
        try {
            return getEnabledStopsByGroupName(START_GROUP + "Nearby");
        }
        catch (TimeoutException notFound) {
            return new ArrayList<>();
        }
    }

    public List<String> getToStops() {
        By toStops = By.id(TO_STOP);
        WebElement elements = waitForClickableLocator(toStops);
        return getEnabledStopNames(elements);
    }

    private List<String> getEnabledStopNames(WebElement groupElement) {
        List<WebElement> stopElements = groupElement.findElements(By.className("stop"));
        return stopElements.stream().filter(WebElement::isEnabled)
                .map(WebElement::getText).
                map(String::trim).collect(Collectors.toList());
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
        createWait().until(webDriver -> elementToBeClickable(element));
    }

    private WebElement waitForClickableLocator(By selector) {
        return createWait().until(elementToBeClickable(selector));
    }

    public boolean notesPresent() {
        return waitForCondition(presenceOfElementLocated(By.id("NotesList")));
    }

    public boolean hasWeekendMessage() {
        return waitForCondition(presenceOfElementLocated(By.id("Weekend")));
    }

    public boolean noWeekendMessage() {
        return waitForCondition(not(
                presenceOfElementLocated(By.id("Weekend"))));
    }

    public String getBuild() {
        return waitForAndGet(By.id("buildNumber"));
    }

    public String getVersion() {
        return waitForAndGet(By.id("dataVersion"));
    }

    private String waitForAndGet(By locator) {
        createWait().until(webDriver -> presenceOfElementLocated(locator));
        return driver.findElement(locator).getText();
    }

    public void displayDisclaimer() {
        WebDriverWait wait = createWait();
        By disclaimerButtonId = By.id("disclaimerButton");
        wait.until(driver -> presenceOfElementLocated(disclaimerButtonId));

        WebElement disclaimerButton = driver.findElement(disclaimerButtonId);
        moveToElement(disclaimerButton).click().perform();
    }

    public boolean waitForDisclaimerVisible() {
        return waitForModalToOpen(By.id(MODAL_DISCLAIMER));
    }

    public boolean waitForDisclaimerInvisible() {
        return waitForModalToClose(By.id(MODAL_DISCLAIMER));
    }

    public boolean waitForCookieAgreementVisible() {
        return waitForModalToOpen(By.id(MODAL_COOKIE_CONSENT));
    }

    public boolean waitForCookieAgreementInvisible() {
        return waitForModalToClose(By.id(MODAL_COOKIE_CONSENT));
    }

    public void dismissDisclaimer() {
        okToModal(By.id(MODAL_DISCLAIMER));
    }

    public void agreeToCookies() {
        okToModal(By.id(MODAL_COOKIE_CONSENT));
    }

    private boolean waitForModalToOpen(By byId) {
        waitForCondition(elementToBeClickable(byId));
        WebElement diag = driver.findElement(byId);
        WebElement button = diag.findElement(By.tagName("button"));
        return waitForCondition(elementToBeClickable(button));
    }

    private boolean waitForModalToClose(By byId) {
        int pauseMs = 400;

        long count = (timeoutInSeconds*1000) / pauseMs;
        try {
            while(true) {
                // will throw once element good
                if (count--<0) {
                    return false;
                }
                driver.findElement(byId);
                Thread.sleep(pauseMs);
            }
        } catch (InterruptedException e) {
           return false;
        } catch (NoSuchElementException expected) {
            return true;
        }
    }

    private boolean waitForCondition(ExpectedCondition<?> expectedCondition) {
        try {
            createWait().until(webDriver -> expectedCondition);
            return true;
        } catch (TimeoutException notShown) {
            return false;
        }
    }

    private void okToModal(By locator) {
        WebElement diag = driver.findElement(locator);
        WebElement button = diag.findElement(By.tagName("button"));
        createWait().until(webDriver -> button.isEnabled());
        moveToElement(button).click().perform();
    }

    public void selectNow() {
        WebElement nowButton = driver.findElement(By.id("nowButton"));
        nowButton.click();
    }

    public void selectToday() {
        WebElement todayButton = driver.findElement(By.id("todayButton"));
        todayButton.click();
    }

    public boolean getArriveBy() {
        WebElement arriveByElement = driver.findElement(By.id("arriveBy"));
        return arriveByElement.isSelected();
    }

    public void setArriveBy(boolean arriveBy) {
        WebElement arriveByElement = driver.findElement(By.id("arriveBy"));
        boolean currently = arriveByElement.isSelected();
        if (currently!=arriveBy) {
            moveToElement(arriveByElement).click().perform();
        }
    }

    private List<String> getEnabledStopsByGroupName(String groupName) {
        WebElement groupElement = waitForClickableLocator(By.id(groupName));
        return getEnabledStopNames(groupElement);
    }

    public boolean hasLocation() {
        return "true".equals(waitForAndGet(By.id("havepos")));
    }

    public boolean waitForReady() {
        // geo loc on firefox can be slow even when stubbing location via a file....
        WebDriverWait wait = new WebDriverWait(driver, 10);
        By plan = By.id(PLAN);
        WebElement element = driver.findElement(plan);

        wait.until(webDriver -> (element.isDisplayed() && element.isEnabled()));

        return element.isDisplayed() && element.isEnabled();
    }

}
