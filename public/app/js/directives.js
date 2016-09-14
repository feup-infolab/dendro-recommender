'use strict';

/* Directives */

angular.module('dendroRecommenderApp.directives', [])
    .directive('focusOn', function() {
        return function(scope, elem, attr) {
            scope.$on('focusOn', function(e, name) {
                //alert("dentro do foco 1" + elem + " name :" + name + " e : " + e);
                if(name === attr.focusOn) {
                    elem[0].focus();
                }
            });
        };
    })
    .directive('datepicker', function() {
        return {
            restrict: 'A',
            require : 'ngModel',
            link: function(scope, element, attrs, ngModelCtrl) {

                element.datetimepicker({
                    dateFormat:'dd/MM/yyyy hh:mm:ss',
                });

                /*.on('changeDate', function(e) {
                    ngModelCtrl.$setViewValue(e.date);
                    scope.$apply();
                });*/
            }
        };
    })
    .directive('json', ['$timeout', '$interpolate', function ($timeout, $interpolate) {
        "use strict";
        return {
            restrict: 'E',
            template: '<pre><code ng-transclude></code></pre>',
            replace: true,
            transclude: true,
            link: function (scope, elm) {
                var tmp = $interpolate(elm.find('code').text())(scope);

                tmp = JSON.stringify(JSON.parse(tmp), undefined, 2);

                elm.find('code').html(hljs.highlightAuto(tmp).value);
            }
        };
    }]);
