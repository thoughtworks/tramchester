package com.tramchester.acceptance.pages;

import org.joda.time.LocalDate;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

public interface ProvidesDateInput {

    String createDateInput(LocalDate localDate);

    void setTime(Actions builder, WebElement element, String input) throws InterruptedException;
}
