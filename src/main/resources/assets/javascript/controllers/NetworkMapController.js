'use strict';

techLabApp.controller('NetworkMapController',
    function MapController(routesService, $scope) {

        var map;

        $scope.drawMap = function () {
            showRoutes();
        };

        function showRoutes() {
            map = L.map('map', {
                layers: MQ.mapLayer(),
                center: [ 53.4774, -2.2309 ], // piccadily station as station
                zoom: 12
            });

            routesService.getAll(function(allRoutes) {
                for (var i = 0; i < allRoutes.length; i++) {
                    displayRoute(allRoutes[i]);
                }
            });
        }

        function displayRoute(route) {
            var points = [];

            var stations = route.stations;
            for (var i = 0; i < stations.length; i++) {
                var station = stations[i];
                var latlng = station.latLong;
                var place = [latlng.lat, latlng.lon];
                L.marker(place, {title: station.name}).addTo(map);
                points.push(place);
            }
            var line = new L.Polyline(points);
            line.addTo(map);
        }

        $scope.$on('$routeChangeSuccess', function () {
            $scope.drawMap();
        });
    }
);