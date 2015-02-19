'use strict';

techLabApp.controller('AdvancedRoutePlannerController',
    function AdvancedRoutePlannerController($scope, mapService, $location) {


        $scope.geoLocate = function (){
           mapService.geoLocate('M15 4TB', getPosition);
        }
        function getPosition(response) {
            if (response.length > 0) {
                var position =  {lat: response[0].lat, lon: response[0].lon};
                alert('yes')
            }
            else {
                var position = null;

            }
        }
    }
);