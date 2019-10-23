package com.tramchester.acceptance.pages.App;

import com.tramchester.acceptance.infra.ProvidesDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class SummaryResult {
    private final LocalTime departTime;
    private final LocalTime arriveTime;
    private final String changes;
    private final WebElement row;
    private final WebElement parent;

    public SummaryResult(WebElement row, WebElement parent) {
        this.row = row;
        this.parent = parent;

        String departTimeTxt = row.findElement(By.className("departTime")).getText();
        departTime = LocalTime.parse(departTimeTxt);
        String arriveTimeTxt = row.findElement(By.className("arriveTime")).getText();
        arriveTime = LocalTime.parse(arriveTimeTxt);
        changes = row.findElement(By.className("changes")).getText();
    }

    public LocalTime getDepartTime() {
        return departTime;
    }

    public LocalTime getArriveTime() {
        return arriveTime;
    }

    public String getChanges() {
        return changes;
    }

    public List<Stage> getStages() {
        List<Stage> stages = new ArrayList<>();
        List<WebElement> rows = parent.findElements(By.className("stageSummary"));
        rows.forEach(row -> stages.add(new Stage(row)));
        return stages;
    }

    public void click(ProvidesDriver providesDriver) {
       providesDriver.click(row);
    }

    public void moveTo(ProvidesDriver providesDriver) {
        providesDriver.moveTo(row);
    }

    public WebElement getElement() {
        return row;
    }
}
