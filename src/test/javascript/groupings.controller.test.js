/* global inject */
describe("GroupingController", () => {

    beforeEach(module("UHGroupingsApp"));
    beforeEach(module("ngMockE2E"));

    let scope;
    let controller;
    let httpBackend;
    let BASE_URL;
    let gs;

    beforeEach(inject(($rootScope, $controller, _BASE_URL_, _$httpBackend_, groupingsService) => {
        scope = $rootScope.$new(true);
        controller = $controller("GroupingsJsController", {
            $scope: scope
        });
        httpBackend = _$httpBackend_;
        BASE_URL = _BASE_URL_;
        gs = groupingsService;
    }));

    it("should define the owner controller", () => {
        expect(controller).toBeDefined();
    });

    describe("init", () => {
        it("should set loading to true can call groupingsService", () => {
            scope.loading = false;
            spyOn(gs, "getGroupingsOwned").and.callThrough();

            scope.init();

            expect(scope.loading).toBeTrue();
            expect(gs.getGroupingsOwned).toHaveBeenCalled();
        });

        it("should remove the selectedOwnerGrouping item from sessionStorage at the end", () => {
            spyOn(window, "open");
            scope.displayOwnerGroupingInNewTab("testPath", "testName");
            scope.init();
            expect(sessionStorage.getItem("selectedOwnerGrouping")).toBeNull();
        });

        it("should call displayOwnerGrouping if displayOwnerGroupingInNewTab was called", () => {
            spyOn(window, "open");
            spyOn(scope, "displayOwnerGrouping");
            scope.displayOwnerGroupingInNewTab("testPath", "testName");
            scope.init();
            expect(scope.displayOwnerGrouping).toHaveBeenCalled();
        });
    });
});
