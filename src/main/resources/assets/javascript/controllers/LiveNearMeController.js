'use strict';

techLabApp.controller('LiveNearMeController',
    function LiveNearMeController($scope, nearby, journeyPlanService) {

        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(getNearStops, positionError,
                {maximumAge: 600000, // 10 minutes cached time on location
                    timeout: 20000  // by default this call never times out...
                });
        } else {
            console.log("Unable to get current position");
        }

        function getNearStops(position) {
            nearby.getNearStops(position.coords.latitude, position.coords.longitude).query(function (stations) {
                $scope.stations = stations;
            });
        }

        function positionError(error) {
            console.log(error);
        }

        $scope.goBack = function (journey) {
            if (journey != null) {
                $location.url('/routeDetails?start=' + journeyPlanService.getStart()
                    + '&end=' + journeyPlanService.getEnd()
                    + '&departureTime=' + journeyPlanService.getDepartureTime());
            } else {
                $location.url('/routePlanner');
            }
        };
    });