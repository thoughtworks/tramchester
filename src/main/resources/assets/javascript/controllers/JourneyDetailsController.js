'use strict';

techLabApp.controller('JourneyDetailsController',
    function JourneyDetailsController(journeyPlanService, $scope, $routeParams, $location) {

        $scope.showMap = function (stage) {
            var stageIndex = $scope.journey.stages.indexOf(stage);
            $location.url('/map/'+$scope.journeyIndex+'/'+stageIndex)
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

        $scope.laterTram = function () {
            var newIndex = 1 + Number($scope.journeyIndex);
            $scope.changeJourneyIndex(newIndex);
        };

        $scope.earlierTram = function () {
            var newIndex = Number($scope.journeyIndex) - 1;
            $scope.changeJourneyIndex(newIndex);
        };

        $scope.changeJourneyIndex = function(newIndex) {
            $scope.journeyIndex = Number(newIndex);
            $scope.journey = journeyPlanService.getJourney(newIndex);
            $scope.disableLaterTram = (newIndex >= (Number($scope.numberOfJourneys)-1));
            $scope.disableEarlierTram = (newIndex < 1);
        };

        $scope.numberOfJourneys = Number(journeyPlanService.numberOfJourneys());
        // routeParams is an angular built-in for URL params
        $scope.changeJourneyIndex($routeParams.journeyIndex);

    }
);