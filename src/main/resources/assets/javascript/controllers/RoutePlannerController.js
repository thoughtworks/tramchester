'use strict';

techLabApp.controller('RoutePlannerController',
    function RoutePlannerController($scope, transportStops, journeyPlanner, $location, journeyPlanService, tramchesterCacheService) {
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

        $scope.fromStopSelected = function() {
           $scope.filterDestinationStop($scope.fromStop);
        }

        $scope.filterDestinationStop = function(selectedId) {
            if (selectedId==null) {
                $scope.endStops = $scope.stops;
            } else {
                $scope.endStops = new Array();
                if ($scope.fromStop != null) {
                    angular.forEach($scope.stops, function (stop) {
                        if (stop.id != selectedId) {
                            $scope.endStops.push(stop);
                        }
                    });
                }
            }
        }

        $scope.groupFilter = function (item) {
            return item.proximityGroup === 'Nearest Stops' || item.proximityGroup === 'All Stops';
        };

        function getNearStops(position) {
            transportStops.getNearStops(position.coords.latitude, position.coords.longitude).query(function (stopList) {
                $scope.stops = stopList;
                $scope.filterDestinationStop($scope.fromStop);
                //$scope.endStops = stopList;
            });
        }

        function getAllStops() {
            transportStops.getAll().query(function (stopList) {
                $scope.stops = stopList;
                $scope.filterDestinationStop($scope.fromStop);
                tramchesterCacheService.addStops("All", stopList);
            });
        }

        function getCurrentTime() {
            var currentDate = new Date();
            return  new Date(currentDate.getYear(), currentDate.getDay(), currentDate.getDay(),
                currentDate.getHours(), currentDate.getMinutes(), 0, 0);
        }
    });
