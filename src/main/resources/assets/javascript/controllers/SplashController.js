techLabApp.controller('SplashController',
    function SplashController($scope, $cookies, $location) {
        $scope.init = function () {
            if ($cookies.get('tramchesterVisited')!=null) {
                $location.path('/routePlanner');
            } 
        };
        $scope.setCookie = function () {
            var cookie = { 'visited' : true };
            var expiry = moment().add(100, 'days').toDate();

            $cookies.putObject('tramchesterVisited',cookie, {'expires': expiry});
        }
    });