'use strict';

techLabApp.factory('transportStops', function ($resource) {
    return {
        getAll: function () {
            return $resource('/api/stations', {});
        },

        getNearStops: function (lat, lon) {
            return $resource('/api/stations/' + lat + '/' + lon, {});
        },

        getTrams: function () {
            return $resource('/api/stations/', {});
        },

        getClosures: function () {
            return $resource('/api/stations/closures/', {});
        }
    };
});
