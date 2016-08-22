'use strict';

techLabApp.factory('journeyPlanService', function () {
    var journeyPlanCache = null;
    var departureTimeCache = null;
    var startCache = null;
    var endCache = null;

    return  {

        setPlan: function (journeyPlan, start, end, departureTime) {
            journeyPlanCache = journeyPlan;
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
                var journey = journeyPlanCache.journeys[journeyIndex];
                return journey;
            }
        },
        
        numberOfJourneys: function() {
            return journeyPlanCache.journeys.length;
        },

        removePlan: function () {
            journeyPlanCache = null;
        }
    };

});