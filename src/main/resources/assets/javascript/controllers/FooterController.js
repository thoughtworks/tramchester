techLabApp.controller('FooterController',
    function FooterController($scope, versionService,feedinfoService) {
        $scope.version = versionService.getVersion();
        $scope.feedinfo = feedinfoService.getVersion();
    });