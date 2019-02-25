'use strict';

techLabApp.controller('MapController',
    function MapController(journeyPlanService, $scope, $routeParams) {
        $scope.journey = journeyPlanService.getJourney($routeParams.journeyIndex);
        $scope.stage = $scope.journey.stages[$routeParams.stageIndex];
        $scope.dest = $scope.stage.lastStation;
        $scope.title = $scope.stage.actionStation.name;
        
        var map, dir;

        $scope.drawMap = function () {
            var stageIndex = $routeParams.stageIndex;

            if (stageIndex==0) {
                if ($scope.stage.mode=="Walk") {
                    navigator.geolocation.getCurrentPosition(showDirections, showActionPlace,
                            {maximumAge: 600000, // 10 minutes cached time on location
                                timeout: 20000  // by default this call never times out...
                            });
                } else {
                    showActionPlace();
                }
            } else {
                if ($scope.stage.mode=="Walk") {
                    showMapDirections(
                        {latLng: {lat: $scope.stage.firstStation.latLong.lat, lng: $scope.stage.firstStation.latLong.lon}},
                        {latLng: {lat: $scope.dest.latLong.lat, lng: $scope.dest.latLong.lon}}
                    );
                } else {
                    showActionPlace();
                }
            }
        };

        function showDirections(currentLocation) {
            showMapDirections(
                {latLng: {lat: currentLocation.coords.latitude, lng: currentLocation.coords.longitude}},
                {latLng: {lat: $scope.dest.latLong.lat, lng: $scope.dest.latLong.lon}})
        }

        function showMapDirections(start, destination) {
            map = L.map('map', {
                layers: MQ.mapLayer(),
                center: [ start.latLng.lat, start.latLng.lng ],
                zoom: 14
            });

            dir = MQ.routing.directions()
                .on('success', function(data) {
                    var legs = data.route.legs,
                        html = '',
                        maneuvers,
                        i;

                    if (legs && legs.length) {
                        html += '<table><tbody>';
                        maneuvers = legs[0].maneuvers;

                        for (i=0; i < maneuvers.length; i++) {
                            html += '<tr>'
                            html += '<td>' + (i+1) + '.</td>';
                            html += '<td>'+ maneuvers[i].narrative + '</td>';
                            html += '</tr>'
                        }
                        html += '</tbody></table>';

                        document.getElementById('narrative').innerHTML = html;
                    }
                });

            dir.route({
                locations: [
                    start,
                    destination
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

        function showActionPlace()  {
            var destLat = $scope.stage.actionStation.latLong.lat;
            var destLon = $scope.stage.actionStation.latLong.lon;
            map = L.map('map', {
                layers: MQ.mapLayer(),
                center: [ destLat, destLon ],
                zoom: 14
            });

            var latLng = {
                lat: destLat,
                lng: destLon
            };

            var popup = L.popup();
            popup.setLatLng(latLng);
            popup.setContent($scope.stage.actionStation.name);
            popup.openOn(map);

            map.setView(latLng);
        }

        $scope.$on('$routeChangeSuccess', function () {
            $scope.drawMap();
        });
    }
);