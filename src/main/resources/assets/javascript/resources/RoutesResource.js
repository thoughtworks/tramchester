'use strict';

techLabApp.factory('routes', function ($resource) {
    return $resource('/api/routes', {});
});