'use strict';

techLabApp.controller('MapController',
    function MapController($scope, mapService, $location) {

        $scope.drawMap = function (position) {
            mapService.drawMap(position.coords.latitude, position.coords.longitude, 16);
            var fromDirection = {lat: position.coords.latitude, lon: position.coords.longitude};
            var toDirection = {lat: $location.search().lat, lon: $location.search().lon};
            if ($location.search().direction == 1) {
                mapService.showDirection(fromDirection, toDirection);
            }
            else {
                mapService.dropPoint(toDirection.lat, toDirection.lon, null, $location.search().name, 'Red');
            }
        };


        $scope.$on('$routeChangeSuccess', function () {
            if (navigator.geolocation) {
                navigator.geolocation.getCurrentPosition($scope.drawMap, function () {
                    mapService.drawManchesterMap();
                });
            } else {
                mapService.drawManchesterMap();
            }
        });
    }
);