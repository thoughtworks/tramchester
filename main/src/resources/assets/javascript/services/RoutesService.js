'use strict';


techLabApp.factory('routesService', function ($http, routes) {
    var routesService = {};

    routesService.getAll = function (callback) {
        if (!routesService.routes) {
            var all = routes.query(function() {
                routesService.routes = all;
                callback(all);
            });
        } else {
            callback(routesService.routes);
        }
    };

    return routesService;
});