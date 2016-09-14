'use strict';

// Declare app level module which depends on filters, and services
var dendroApp = angular.module('dendroRecommenderApp', [
        'ngRoute',
        'dendroRecommenderApp.controller',
        'dendroRecommenderApp.services',
        'dendroRecommenderApp.directives',
        'ngAnimate'
    ]);

dendroApp.config(['$routeProvider',
    function($routeProvider) {
        $routeProvider.
            //Users
            when('/interactions', {
                templateUrl: '/public/app/views/interactions/interactions.html',
                controller: 'interactionsCtrl'
            }).
            //Recommendations
            when('/recommendations', {
                templateUrl: '/public/app/views/recommendations/recommendations.html',
                controller: 'recommendationsCtrl'
            }).
            //Static pages
            when('/about', {
                templateUrl: '/public/app/views/about/about.html'
            }).
            when('/',{
                templateUrl: '/public/app/views/home/home.html',
                controller: 'homeCtrl'
            }).
            otherwise({
                templateUrl: '/public/app/views/error/error.html'
            });
    }]);