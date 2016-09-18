'use strict';

techLabApp.factory('feedinfoService', function ($http, feedinfo) {
    var feedinfoService = {};

    feedinfoService.getVersion = function () {
        if (!feedinfoService.feedinfoService) {
            feedinfoService.feedinfoService = feedinfo.get();
        }
        return feedinfoService.feedinfoService;
    };

    return feedinfoService;
});