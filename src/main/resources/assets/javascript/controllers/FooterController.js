techLabApp.controller('FooterController',
    function FooterController($scope, versionService, feedinfoService) {
        $scope.version = versionService.getVersion();
        feedinfoService.getFeedInfo(function(info){
            $scope.feedinfo = info;
        });
    });