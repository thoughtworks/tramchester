'use strict';

techLabApp.controller('LiveNearMeController',
    function LiveNearMeController($scope, $sce, nearby) {

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

        $scope.safeHtml = function(html) {
            return $sce.trustAsHtml(html);
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
                $scope.notes = stationList.notes;
                $scope.Notes = ($scope.notes.length != 0);
                $scope.departures = null;
            });
        }

        function getNearDepartures(position) {
            nearby.getNearDepartures(position.coords.latitude, position.coords.longitude).get(function (departureList) {
                $scope.departures = departureList.departures;
                $scope.notes = departureList.notes;
                $scope.stations = [];
                $scope.Notes = ($scope.notes.length != 0);
            });
        }

        function positionError(error) {
            console.log(error);
        }
    });