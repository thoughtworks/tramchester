'use strict';


techLabApp.factory('feedinfo', function($resource){
    return  $resource('/api/feedinfo', {});
});
