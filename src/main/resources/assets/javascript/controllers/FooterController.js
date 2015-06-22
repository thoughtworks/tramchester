techLabApp.controller('FooterController',
    function FooterController($scope, versionService) {
        $scope.version = versionService.getVersion();
    });