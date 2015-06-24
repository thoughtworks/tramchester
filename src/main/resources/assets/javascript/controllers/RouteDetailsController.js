'use strict';

techLabApp.controller('RouteDetailsController',
    function RouteDetailsController($scope, $routeParams, journeyPlanner, $location, journeyPlanService, transportStops) {
        var start = $location.search().start;
        var end = $location.search().end;
        var departureTime = $location.search().departureTime;
        transportStops.getClosures().get(function(closure){
           $scope.closure = closure;
        });

        if (journeyPlanService.getPlan() == null) {
            journeyPlanner.quickestRoute(start, end, departureTime).get(function (journeyPlan) {
                journeyPlanService.setPlan(journeyPlan, start, end, departureTime);
                $scope.journeyPlan = journeyPlanService.getPlan();
                if($scope.journeyPlan.journeys.length == 0){
                    $scope.NoRoutes = true;
                }
            });
        }
        else {
            $scope.journeyPlan = journeyPlanService.getPlan();
            if($scope.journeyPlan.journeys.length == 0){
                $scope.NoRoutes = true;
            }
        }

        $scope.showJourneyDetails = function (journeyIndex) {
            $location.url('/journeyDetails/' + journeyIndex);
        };

        $scope.goBack = function () {
            $location.url('/routePlanner/');

        };

    }
);