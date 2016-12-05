'use strict';

var techLabApp = angular.module('techLabApp', ['ngResource','ngRoute', 'ngCookies']).
    config(function ($routeProvider, $locationProvider) {
        $routeProvider.
            when('/', {templateUrl: 'templates/splash.html', controller: 'SplashController'}).
            when('/routePlanner', { templateUrl: 'templates/RoutePlanner.html', controller: 'RoutePlannerController'}).
            when('/routeDetails', {templateUrl: 'templates/RouteDetails.html', controller: 'RouteDetailsController'}).
            when('/disclaimer', {templateUrl: 'templates/Disclaimer.html'}).
            when('/map/:journeyIndex/:stageIndex', {templateUrl: 'templates/map.html', controller: 'MapController'}).
            when('/networkMap', {templateUrl: 'templates/networkMap.html', controller: 'NetworkMapController'}).
            when('/about', {templateUrl: 'templates/about.html', controller: 'AboutController'}).
            when('/journeyDetails/:journeyIndex', { templateUrl: 'templates/JourneyDetails.html',
                controller: 'JourneyDetailsController' })
    }).directive('ngModel', function( $filter ) {
        return {
            require: '?ngModel',
            link: function(scope, elem, attr, ngModel) {
                if( !ngModel )
                    return;
                if( attr.type !== 'time' )
                    return;
                // work around time display issue on some devices/browsers
                ngModel.$formatters.unshift(function(value) {
                    return value.replace(/:00\.000$/, '')
                });
            }
        }
    });