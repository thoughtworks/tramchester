'use strict';

techLabApp.controller('MapController',
    function MapController($scope, $location) {
        var map, dir;

        $scope.drawMap = function () {
            if ($location.search().showDirections==1) {
                navigator.geolocation.getCurrentPosition(showDirections, showStation,
                    {maximumAge: 600000, // 10 minutes cached time on location
                        timeout: 20000  // by default this call never times out...
                    });
            } else {
                showStation();
            }
        };

        function showDirections(position) {
            map = L.map('map', {
                layers: MQ.mapLayer(),
                center: [ position.coords.latitude, position.coords.longitude ],
                zoom: 14
            });

            dir = MQ.routing.directions();

            dir.route({
                locations: [
                    {latLng: {lat: position.coords.latitude, lng: position.coords.longitude}},
                    {latLng: {lat: $location.search().lat, lng: $location.search().lon}}
                ],
                options: {
                    routeType: "pedestrian",
                    unit: "M"
                }
            });

            map.addLayer(MQ.routing.routeLayer({
                directions: dir,
                fitBounds: true
            }));
        }

        function showStation()  {
            map = L.map('map', {
                layers: MQ.mapLayer(),
                center: [ $location.search().lat, $location.search().lon ],
                zoom: 14
            });

            var latLng = {
                lat: $location.search().lat,
                lng: $location.search().lon
            };

            var popup = L.popup();
            popup.setLatLng(latLng);
            popup.setContent($location.search().name);
            popup.openOn(map);

            map.setView(latLng);
        }

        $scope.$on('$routeChangeSuccess', function () {
            $scope.drawMap();
        });
    }
);