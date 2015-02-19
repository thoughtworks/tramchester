'use strict';

var techLabApp = angular.module('techLabApp', ['ngResource']).
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
            when('/liveMap', {templateUrl: 'templates/LiveMap.html', controller: 'LiveMapController'}).
            when('/', {templateUrl: 'templates/splash.html'}).
            when('/disclaimer', {templateUrl: 'templates/Disclaimer.html'}).
            when('/map', {templateUrl: 'templates/map.html', controller: 'MapController'}).
            when('/about', {templateUrl: 'templates/about.html', controller: 'AboutController'}).
            when('/journeyDetails/:journeyIndex', {
                templateUrl: 'templates/JourneyDetails.html',
                controller: 'JourneyDetailsController'
            })//.
        //otherwise({redirectTo: '/'});
    });