techLabApp.controller('SplashController',
    function SplashController($scope, $cookies, $location) {
        $scope.init = function () {
            if ($cookies.get('tramchesterVisited')!=null) {
                $location.path('/routePlanner');
            } 
        };
        $scope.setCookie = function () {
            var cookie = { 'visited' : true };
            $cookies.putObject('tramchesterVisited',cookie);
        }
    });