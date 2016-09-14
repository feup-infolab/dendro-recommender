angular.module('dendroRecommenderApp.controller', [])
    .controller('recommendationsCtrl', function ($scope, $http, $filter) {
        $http.defaults.headers.common.Accept = 'application/json';
        //$scope.number_or_recommendations = 5;
        $scope.descriptor_filter = "hidden";

        $scope.page = "0";
        $scope.page_size = "30";

        //$scope.userUri = "http://dendro-prd.fe.up.pt:3007/user/fermjf";
        //$scope.resourceUri = "http://dendro-prd.fe.up.pt:3007/project/hydrogen01/data/classic hydrolysis";

        //$scope.userUri = "http://dendro-prd.fe.up.pt:3007/user/ricardoamorim";
        //$scope.resourceUri = "http://dendro-prd.fe.up.pt:3007/project/labtablet";

        //$scope.userUri = "http://dendro-prd.fe.up.pt:3007/user/martinadl";
        //$scope.resourceUri = "http://dendro-prd.fe.up.pt:3007/project/ocebio01/data/Nassas e arrastos Corbicula fluminea";

        //testing hidden
        $scope.userUri = "http://dendro-prd.fe.up.pt:3007/user/lcherri";
        $scope.resourceUri = "http://dendro-prd.fe.up.pt:3007/project/cuttingpacking/data/2D cutting and packing/relatório_concentracção_h5n1.pdf";

        $scope.get_recommendations = function()
        {
            $http( {
                url : "/recommendations/recommend",
                method : "GET",
                params : {
                    descriptor_filter : $scope.descriptor_filter,
                    number_of_recommendations : $scope.number_or_recommendations,
                    page : $scope.page,
                    page_size : $scope.page_size,
                    user : $scope.userUri,
                    current_resource : $scope.resourceUri
                }
            }).success(function(data) {
                $scope.recommendations = data.recommendations;
            });
        };

        $scope.get_recommendations();
    })
    .controller('interactionsCtrl', function ($scope, $http, $filter) {
        $http.defaults.headers.common.Accept = 'application/json';
        $scope.number_of_recent_interactions = 10;

        $scope.get_interactions = function()
        {
            var requestUri= "/interactions/latest/" + $scope.number_of_recent_interactions;
            $http.get(requestUri).success(function(data) {
                $scope.interactions = data.interactions;
            });
        }

        $scope.get_interactions();
    })
    .controller('homeCtrl', function ($scope, $http, $filter) {

    });

//$.pnotify.defaults.styling = "bootstrap3";
//$.pnotify.defaults.history = false;