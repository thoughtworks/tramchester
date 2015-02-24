'use strict';

techLabApp.factory('journeyPlanService', function () {
    var journeyPlanCache = null;
    var departureTimeCache = null;
    var startCache = null;
    var endCache = null;

    return  {

        setPlan: function (journeyPlan, start, end, departureTime) {
            journeyPlanCache = journeyPlanFormatter(journeyPlan);
            departureTimeCache = departureTime;
            startCache = start;
            endCache = end;
        },

        getDepartureTime: function () {
            return departureTimeCache;
        },

        getPlan: function () {
            return journeyPlanCache;
        },

        getEnd: function () {
            return endCache;
        },

        getStart: function () {
            return startCache;
        },

        getJourney: function (journeyIndex) {
            if (journeyPlanCache != null) {
                return journeyPlanCache.journeys[journeyIndex];
            }
        },

        removePlan: function () {
            journeyPlanCache = null;
        }
    };

    function getStop(stops, stopId) {
        var length = stops.length,
            element = null;
        for (var i = 0; i < length; i++) {
            element = stops[i];
            if (element.id === stopId) {
                return element;
            }
        }
    }

    function journeyPlanFormatter(journeyPlan) {

        for (var i = 0; i < journeyPlan.journeys.length; i++) {
            for (var j = 0; j < journeyPlan.journeys[i].stages.length; j++) {
                journeyPlan.journeys[i].stages[j].beginStop = getStop(journeyPlan.stations, journeyPlan.journeys[i].stages[j].firstStation);
                journeyPlan.journeys[i].stages[j].endStop = getStop(journeyPlan.stations, journeyPlan.journeys[i].stages[j].lastStation);
            }
        }
        return journeyPlan;
    }
});