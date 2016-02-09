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

        //getNumberOfServiceTimes: function(journeyIndex) {
        //    var journey = journeyPlanCache.journeys[journeyIndex];
        //    return journey.stages[0].serviceTimes.length;
        //},

        getJourney: function (journeyIndex) {
            if (journeyPlanCache != null) {
                var journey = journeyPlanCache.journeys[journeyIndex];
                return journey;
            }
        },

        removePlan: function () {
            journeyPlanCache = null;
        }
    };

    //function getStop(stops, stopId) {
    //    var length = stops.length,
    //        element = null;
    //    for (var i = 0; i < length; i++) {
    //        element = stops[i];
    //        if (element.id === stopId) {
    //            return element;
    //        }
    //    }
    //}

    function journeyPlanFormatter(journeyPlan) {
        for (var i = 0; i < journeyPlan.journeys.length; i++) {
            var journey = journeyPlan.journeys[i];
            for (var stageIndex = 0; stageIndex < journey.stages.length; stageIndex++) {
                //journey.stages[stageIndex].beginStop = getStop(journeyPlan.stations, journey.stages[stageIndex].firstStation);
                //journey.stages[stageIndex].endStop = getStop(journeyPlan.stations, journey.stages[stageIndex].lastStation);//
                journey.stages[stageIndex].beginStop = journey.stages[stageIndex].firstStation;
                journey.stages[stageIndex].endStop = journey.stages[stageIndex].lastStation;
            }
        }
        return journeyPlan;
    }
});