'use strict';

techLabApp.controller('RoutePlannerController',
    function RoutePlannerController($scope, transportStops, journeyPlanner, $location, journeyPlanService) {
        $scope.selectedStop = null;
        $scope.departureTime = getCurrentTime();
        $scope.fromStop = journeyPlanService.getStart();
        $scope.toStop = journeyPlanService.getEnd();

        journeyPlanService.removePlan();

        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(getNearStops, function () {
                getAllStops();
            }, {
                maximumAge: 600000 // cached time on location
            });
        } else {
            getAllStops();
        }

        $scope.findRoute = function (fromStop, toStop, departureTime, journeyPlanForm) {
            if (journeyPlanForm.$valid) {
                var hour = departureTime.getHours() > 9 ? departureTime.getHours().toString() : "0" + departureTime.getHours().toString();
                var minutes = departureTime.getMinutes() > 9 ? departureTime.getMinutes().toString() : "0" + departureTime.getMinutes().toString();
                var time=  hour + ":" + minutes;
                $location.url('/routeDetails?start=' + fromStop + '&end=' + toStop + "&departureTime=" + time);
            }
        }

        function getNearStops(position) {
            transportStops.getNearStops(position.coords.latitude, position.coords.longitude).query(function (stopList) {
                $scope.stops = stopList;
            });
        }

        function getAllStops() {
            transportStops.getAll().query(function (stopList) {
                $scope.stops = stopList;
                tramchesterCacheService.addStops("All", stopList);
            });
        }

        function getCurrentTime() {
            var currentDate = new Date();
            return  new Date(currentDate.getYear(), currentDate.getDay(), currentDate.getDay(),
                currentDate.getHours(), currentDate.getMinutes(), 0, 0);
        }
    });
