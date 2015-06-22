'use strict';

techLabApp.factory('versionService', function ($http, version) {
    var versionService = {};

    versionService.getVersion = function () {
        if (!versionService.versionDetails) {
            versionService.versionDetails = version.get();
        }
        return versionService.versionDetails;
    };

    return versionService;
});