'use strict';

techLabApp.controller('JourneyDetailsController',
    function JourneyDetailsController(journeyPlanService, $scope, $routeParams, $location) {
        $scope.journey = journeyPlanService.getJourney($routeParams.journeyIndex);
        $scope.serviceTimeIndex = 0;
        $scope.disableNextTram = false;
        $scope.disablePreviousTram = true;
        $scope.showChangeIndicator = false;

        $scope.showMap = function (stage) {
            var latLong = stage.firstStation.latLong;
            if ($scope.journey.stages.indexOf(stage) == 0) {
                $location.url('/map?lat=' + latLong.lat + "&lon=" + latLong.lon
                    + "&name=" + stage.firstStation.name + "&showDirections=1");
            } else {
                $location.url('/map?lat=' + latLong.lat + "&lon=" + latLong.lon
                    + "&name=" + stage.firstStation.name + "&showDirections=0");
            }
        };

        $scope.goBack = function (journey) {
            if (journey != null) {
                $location.url('/routeDetails?start=' + journeyPlanService.getStart()
                    + '&end=' + journeyPlanService.getEnd()
                    + '&departureTime=' + journeyPlanService.getDepartureTime());
            } else {
                $location.url('/routePlanner');
            }
        };

        $scope.nextTram = function (journey) {
            if ($scope.serviceTimeIndex < journey.numberOfTimes-1) {
                $scope.serviceTimeIndex++;
                $scope.disablePreviousTram = false;
            } else {
                $scope.disableNextTram = true;
            }
        };

        $scope.previousTram = function () {
            if ($scope.serviceTimeIndex > 0) {
                $scope.serviceTimeIndex--;
                $scope.disableNextTram = false;
            } else {
                $scope.disablePreviousTram = true;
            }
        }
    }
);