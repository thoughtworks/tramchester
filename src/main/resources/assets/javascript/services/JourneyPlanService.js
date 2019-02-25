'use strict';

techLabApp.factory('journeyPlanService', function () {
    var journeyPlanCache = null;
    var departureTimeCache = null;
    var departureDateCache = null;
    var startCache = null;
    var endCache = null;

    return  {

        setPlan: function (journeyPlan, start, end, departureTime, departureDate) {
            journeyPlanCache = journeyPlan;
            departureTimeCache = departureTime;
            departureDateCache = departureDate;
            startCache = start;
            endCache = end;
        },

        getDepartureTime: function () {
            return departureTimeCache;
        },

        getDepartureDate: function () {
            return departureDateCache;
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