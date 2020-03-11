package com.tramchester.acceptance.pages.App;

import com.tramchester.acceptance.pages.Page;
import com.tramchester.acceptance.pages.ProvidesDateInput;
import com.tramchester.testSupport.Stations;
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
    private static final String MODAL_COOKIE_CONSENT = "modal-cookieConsent";
    private static final String MODAL_DISCLAIMER = "modal-disclaimer";
    private static final String PLAN = "plan";
    private final ProvidesDateInput providesDateInput;
    private long timeoutInSeconds = 15;

    private static final String DATE = "date";
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

    public boolean waitForToStops() {
        WebDriverWait wait = createWait();
        wait.until(webDriver -> ExpectedConditions.presenceOfElementLocated(By.id(FROM_STOP)));
        WebElement fromStopElement = findElementById(FROM_STOP);
        wait.until(webDriver -> ExpectedConditions.textToBePresentInElement(fromStopElement, Stations.ManAirport.getName()));
        return fromStopElement.isEnabled() && fromStopElement.isDisplayed();
    }

    public void planAJourney() {
        //findPlanButton().click();
        findAndClickElement(PLAN);
    }

    private void findAndClickElement(String elementId) {
        WebElement arriveByElement = driver.findElement(By.id(elementId));
        Actions actions = new Actions(driver);
        actions.moveToElement(arriveByElement).click().perform();
    }

    public void earlier() {
        findAndClickElement("earlierButton");
    }

    public void later() {
        findAndClickElement("laterButton");
    }

    public boolean searchEnabled() {
        WebElement planButton = findElementById(PLAN);
        createWait().until(ExpectedConditions.elementToBeClickable(planButton));
        return planButton.isEnabled();
    }

    public void setStart(String start) {
        setSelector(start, FROM_STOP);
    }

    public void setDest(String destination) {
        setSelector(destination, TO_STOP);
    }

    private void setSelector(String start, String id) {
        WebElement element = driver.findElement(By.id(id));
        Actions actions = new Actions(driver);
        actions.moveToElement(element).perform();

        Select selector = new Select(element);
        selector.selectByVisibleText(start);
    }

    public void setDate(LocalDate localDate) {

        WebElement element = getDateElement();

        String input = providesDateInput.createDateInput(localDate);

        Actions actions = new Actions(driver);
        actions.moveToElement(element).click().perform();
        actions.sendKeys(element, Keys.ARROW_LEFT).perform();
        actions.sendKeys(element, input).perform();

        // space to close picker
        actions.sendKeys(element, Keys.SPACE).perform();
    }

    public void setTime(LocalTime time) {
        WebElement element = getTimeElement();

        Actions builder  = new Actions(driver);
        String input = providesDateInput.createTimeFormat(time);
        int chars = input.length();

        builder.moveToElement(element);
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
        By resultsById = By.id(RESULTS);
        createWait().until(ExpectedConditions.elementToBeClickable(resultsById));

        WebElement resultsDiv = driver.findElement(resultsById);
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
        createWait().until(webDriver -> ExpectedConditions.elementToBeClickable(element));
    }

    private void waitForClickableLocator(By selector) {
        createWait().until(webDriver -> ExpectedConditions.elementToBeClickable(selector));
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
        createWait().until(webDriver -> ExpectedConditions.presenceOfElementLocated(locator));
        return driver.findElement(locator).getText();
    }

    public void displayDisclaimer() {
        WebDriverWait wait = createWait();
        By disclaimerButtonId = By.id("disclaimerButton");
        wait.until(driver -> ExpectedConditions.presenceOfElementLocated(disclaimerButtonId));

        WebElement disclaimerButton = driver.findElement(disclaimerButtonId);
        Actions action = new Actions(driver);
        action.moveToElement(disclaimerButton).click().perform();
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
        waitForCondition(ExpectedConditions.elementToBeClickable(byId));
        WebElement diag = driver.findElement(byId);
        WebElement button = diag.findElement(By.tagName("button"));
        return waitForCondition(ExpectedConditions.elementToBeClickable(button));
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
        Actions actions = new Actions(driver);
        actions.moveToElement(button).click().perform();
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
            Actions actions = new Actions(driver);
            actions.moveToElement(arriveByElement).click().perform();
        }
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

    public boolean waitForReady() {
        // geo loc on firefox can be slow even when stubbing location via a file....
        WebDriverWait wait = new WebDriverWait(driver, 10);
        By plan = By.id(PLAN);
        WebElement element = driver.findElement(plan);

        wait.until(webDriver -> (element.isDisplayed() && element.isEnabled()));

        return element.isDisplayed() && element.isEnabled();
    }

}
