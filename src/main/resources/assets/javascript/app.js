'use strict';



var techLabApp = angular.module('techLabApp', ['ngResource','ngRoute']).
    config(function ($routeProvider, $locationProvider) {
        $routeProvider.when('/routePlanner', {
            templateUrl: 'templates/RoutePlanner.html',
            controller: 'RoutePlannerController'
        }).
            when('/advancedRoutePlanner', {
                templateUrl: 'templates/AdvancedRoutePlanner.html',
                controller: 'AdvancedRoutePlannerController'
            }).
            when('/routeDetails', {templateUrl: 'templates/RouteDetails.html', controller: 'RouteDetailsController'}).
            when('/', {templateUrl: 'templates/splash.html'}).
            when('/disclaimer', {templateUrl: 'templates/Disclaimer.html'}).
            when('/map', {templateUrl: 'templates/map.html', controller: 'MapController'}).
            when('/about', {templateUrl: 'templates/about.html', controller: 'AboutController'}).
            when('/journeyDetails/:journeyIndex', {
                templateUrl: 'templates/JourneyDetails.html',
                controller: 'JourneyDetailsController'
            })//.
        //otherwise({redirectTo: '/'});
    }).directive('ngModel', function( $filter ) {
        return {
            require: '?ngModel',
            link: function(scope, elem, attr, ngModel) {
                if( !ngModel )
                    return;
                if( attr.type !== 'time' )
                    return;

                ngModel.$formatters.unshift(function(value) {
                    return value.replace(/:00\.000$/, '')
                });
            }
        }
    });