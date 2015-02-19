'use strict';

techLabApp.factory('transportStops', function($resource){
    return {
        getAll: function(){
            return $resource('/service/transportStop', {});
        },

        getNearStops: function(lat, lon){
            return $resource('/service/transportStop/'+ lat + '/' + lon, {});
        },

        getTrams: function(){
            return $resource('/service/transportTrams/', {});
        }
    };
});
