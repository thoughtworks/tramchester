'use strict';

techLabApp.factory('feedinfoService', function ($http, feedinfo) {
    var feedinfoService = {};

    feedinfoService.getFeedInfo = function (callback) {
        if (!feedinfoService.feedinfoDetails) {
            var info = feedinfo.get(function() {
                feedinfoService.feedinfoDetails = info;
                callback(info);
            });
        } else {
            callback(feedinfoService.feedinfoDetails);
        }
    };

    return feedinfoService;
});