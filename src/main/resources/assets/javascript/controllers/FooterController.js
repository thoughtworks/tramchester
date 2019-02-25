techLabApp.controller('FooterController',
    function FooterController($scope, versionService, feedinfoService) {
        versionService.getVersion(function (version) {
            $scope.version = version;
        });
        feedinfoService.getFeedInfo(function(info){
            $scope.feedinfo = info;
        });
    });