'use strict';

techLabApp.factory('nearby', function ($resource) {
    return {
        getNearStops: function (lat, lon) {
            return $resource('/api/stations/live/' + lat + '/' + lon, {});
        }
    };
});