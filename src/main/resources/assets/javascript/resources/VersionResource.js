'use strict';

techLabApp.factory('version', function($resource){
    return  $resource('/api/version', {});
});