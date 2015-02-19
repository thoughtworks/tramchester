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
            });
        } else {
            getAllStops();
        }

        $scope.findRoute = function (fromStop, toStop, departureTime, journeyPlanForm) {
            if (journeyPlanForm.$valid) {
                $location.url('/routeDetails?start=' + fromStop + '&end=' + toStop + "&departureTime=" + departureTime);
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
            var hour = currentDate.getHours() > 9 ? currentDate.getHours().toString() : "0" + currentDate.getHours().toString();
            var minutes = currentDate.getMinutes() > 9 ? currentDate.getMinutes().toString() : "0" + currentDate.getMinutes().toString();
            return hour + ":" + minutes;
        }
    });