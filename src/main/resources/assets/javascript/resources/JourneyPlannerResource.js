'use strict';

techLabApp.factory('journeyPlanner', function($resource){
    return {
        quickestRoute: function(fromStop, toStop, departureTime){
            departureTime = departureTime + ":00";
            return $resource('/api/journey', {start:fromStop, end:toStop, departureTime: departureTime});
        }
    };
});
