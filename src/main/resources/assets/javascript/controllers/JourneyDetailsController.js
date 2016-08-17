'use strict';

techLabApp.controller('JourneyDetailsController',
    function JourneyDetailsController(journeyPlanService, $scope, $routeParams, $location) {
        // routeParams is an angular built-in for URL params
        $scope.journey = journeyPlanService.getJourney($routeParams.journeyIndex);
        $scope.disableNextTram = false;
        $scope.disablePreviousTram = true;
        $scope.showChangeIndicator = false;

        $scope.showMap = function (stage) {
            var stageIndex = $scope.journey.stages.indexOf(stage);
            var journeyIndex = $routeParams.journeyIndex;
            $location.url('/map/'+journeyIndex+'/'+stageIndex)
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
    }
);