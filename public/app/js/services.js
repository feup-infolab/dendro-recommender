'use strict';

/* Services */

// Demonstrate how to register services
// In this case it is a simple value service.
angular.module('dendroRecommenderApp.services', [])
    .factory('InteractionService', function($scope){
        return {
            get_latest_interactions : function(size)
            {
                var requestUri= "/interactions/latest/" + size;

                $http.defaults.headers.common.Accept = 'application/json';

                $http.get(requestUri).success(function(data) {
                    $scope.interactions.latest = data;
                });
            }
        };
    });
