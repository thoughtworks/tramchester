'use strict';

techLabApp.factory('versionService', function ($http, version) {
    var versionService = {};

    versionService.getVersion = function (callback) {
        if (!versionService.versionDetails) {
            var result = version.get(function() {
                versionService.versionDetails = result;
                callback(result);
            });
        } else {
            callback(versionService.versionDetails);
        }
    };

    return versionService;
});