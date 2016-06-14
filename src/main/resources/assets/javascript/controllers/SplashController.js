techLabApp.controller('SplashController',
    function SplashController($scope, $cookies, $location) {
        $scope.init = function () {
            if ($cookies.get('tramchesterVisited')==null) {
                var cookie = { 'visited' : true };
                $cookies.putObject('tramchesterVisited',cookie);
            } else {
                $location.path('/routePlanner');
            }
        };
    });