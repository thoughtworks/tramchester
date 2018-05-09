'use strict';

techLabApp.controller('LiveNearMeController',
    function LiveNearMeController($scope, nearby, journeyPlanService) {

        $scope.flag = true; // true = stations, false = departures
        refresh();

        function refresh() {
            if (navigator.geolocation) {
                navigator.geolocation.getCurrentPosition(queryForData, positionError,
                    {
                        maximumAge: 600000, // 10 minutes cached time on location
                        timeout: 20000  // by default this call never times out...
                    });
            } else {
                console.log("Unable to get current position");
            }
        }

        $scope.switchView = function () {
            $scope.flag = !$scope.flag;
            refresh();
        };

        function queryForData(position) {
            if ($scope.flag) {
                getNearStops(position);
            } else {
                getNearDepartures(position);
            }
        }

        function getNearStops(position) {
            nearby.getNearStops(position.coords.latitude, position.coords.longitude).get(function (stationList) {
                $scope.stations = stationList.stations;
                $scope.departures = [];
            });
        }

        function getNearDepartures(position) {
            nearby.getNearDepartures(position.coords.latitude, position.coords.longitude).get(function (departures) {
                $scope.departures = departures;
                $scope.stations = [];

            });
        }

        function positionError(error) {
            console.log(error);
        }
    });