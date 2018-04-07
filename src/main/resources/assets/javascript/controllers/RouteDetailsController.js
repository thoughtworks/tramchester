'use strict';

techLabApp.controller('RouteDetailsController',
    function RouteDetailsController($scope, $sce, $routeParams, journeyPlanner, $location, journeyPlanService, transportStops) {
        var start = $location.search().start;
        var end = $location.search().end;
        var departureTime = $location.search().departureTime;
        var departureDate = $location.search().departureDate;

        transportStops.getClosures().get(function(closure){
           $scope.closure = closure;
        });

        if (journeyPlanService.getPlan() == null) {
            journeyPlanner.quickestRoute(start, end, departureTime, departureDate).get(function (journeyPlan) {
                journeyPlanService.setPlan(journeyPlan, start, end, departureTime, departureDate);
                $scope.journeyPlan = journeyPlanService.getPlan();
                if($scope.journeyPlan.journeys.length == 0){
                    $scope.NoRoutes = true;
                }
                if($scope.journeyPlan.notes.length != 0) {
                    $scope.Notes = true;
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
        
        $scope.safeHtml = function(html) {
            return $sce.trustAsHtml(html);
        }

    }
);