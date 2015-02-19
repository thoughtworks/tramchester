'use strict';

techLabApp.controller('LiveMapController',
    function LiveMapController($scope, mapService, transportStops, $timeout) {
        $scope.count = 1;
        var tramsPoints = null;


        transportStops.getAll().query(function (stopList) {
            $scope.stops = stopList;
            mapService.drawStops($scope.stops);
            getTrams();

        });


        $scope.$on('$routeChangeSuccess', function () {
            mapService.drawManchesterMap();
            mapService.showControls();

        });

        function getTrams() {
            $scope.count = $scope.count + 1;
            transportStops.getTrams().query(function (trams) {
                $scope.trams = trams;
                tramsPoints = mapService.drawTrams(trams, tramsPoints);
            });
            $timeout(getTrams, 5000);

        }


    }
);