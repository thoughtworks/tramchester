package com.tramchester.acceptance.pages.App;

import com.tramchester.acceptance.infra.ProvidesDriver;
import com.tramchester.domain.time.TramTime;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

public class TestResultSummaryRow {
    private final TramTime departTime;
    private final TramTime arriveTime;
    private final String changes;
    private final WebElement row;
    private final WebElement parent;

    @Override
    public String toString() {
        return "SummaryResult{" +
                "departTime=" + departTime +
                ", arriveTime=" + arriveTime +
                ", changes='" + changes + '\'' +
                '}';
    }

    public TestResultSummaryRow(WebElement row, WebElement parent) {
        this.row = row;
        this.parent = parent;

        String departTimeTxt = row.findElement(By.className("departTime")).getText();
        departTime = parse(departTimeTxt);
        String arriveTimeTxt = row.findElement(By.className("arriveTime")).getText();
        arriveTime = parse(arriveTimeTxt);
        changes = row.findElement(By.className("changes")).getText();
    }

    private TramTime parse(final String original) {
        String timeString = original.
                replace(" +1d","+24").
                trim();
        if (timeString.startsWith("24:")) {
            timeString = timeString.replace("24:", "00:"); // chrome time display issue
        }
        TramTime result = TramTime.parse(timeString);
        if (!result.isValid()) {
            throw new RuntimeException(String.format("Could not parse '%s' to a valid time, intermediate was '%s'",
                    original, timeString));
        }
        return result;
    }

    public TramTime getDepartTime() {
        return departTime;
    }

    public TramTime getArriveTime() {
        return arriveTime;
    }

    public String getChanges() {
        return changes;
    }

    public List<Stage> getStages() {
        List<Stage> stages = new ArrayList<>();
        // expand the row
        row.findElement(By.tagName("button")).click();
        // find the stages
        WebElement stagesElement = parent.findElement(By.id("stages"));
        List<WebElement> rows = stagesElement.findElements(By.className("stageSummary"));
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
