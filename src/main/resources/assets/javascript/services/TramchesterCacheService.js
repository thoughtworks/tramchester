'use strict';

techLabApp.factory('tramchesterCacheService', function ($cacheFactory) {
    var journeyPlanCache = $cacheFactory('tramchesterJourneyPlanCache', {capacity: 3});
    var stopsCache = $cacheFactory('tramchesterStopsCache', {capacity: 2});
    return  {

        addStops: function (key,stops){
            stopsCache.put(key, stops);
        },

        getStops: function(key){
            return stopsCache.get(key);
        },

        addJourneyPlan: function(key, journeyPlan){
            journeyPlanCache.put(key, journeyPlan);
        },

        getJourneyPlan: function(key){
            return journeyPlanCache.get(key);
        }


};
});