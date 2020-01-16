
var moment = require('moment');
const axios = require('axios');
var _ = require('lodash');
var Vue = require('vue');
Vue.use(require('vue-cookies'));
Vue.use(require('bootstrap-vue'));

import 'bootstrap/dist/css/bootstrap.css';
import 'bootstrap-vue/dist/bootstrap-vue.css';
import './../css/tramchester.css'

function getCurrentTime() {
    return moment().format("HH:mm");
}

function getCurrentDate() {
    return moment().format("YYYY-MM-DD")
}

function stationsUrl(app) {
    var base = '/api/stations';
    if (!app.hasGeo) {
        return base;
    }
    var place = app.location;
    if (place!=null) {
        return base + '/' + place.coords.latitude + '/' + place.coords.longitude;
    }
    return base;
}

function livedataUrlFromLocation(app) {
    var place = app.location; // should not have location place holder without a valid location
    return '/api/departures/' + place.coords.latitude + '/' + place.coords.longitude;
}

function livedataUrl(app) {
    if (app.startStop==null || app.startStop==='MyLocationPlaceholderId') {
        return livedataUrlFromLocation(app);
    } else {
        return '/api/departures/station/'+app.startStop;
    }
}

function displayLiveData(app) {
    var queryDate = moment(app.date, "YYYY-MM-DD");
    var today = moment();
    if (today.month()==queryDate.month()
        && today.year()==queryDate.year()
        && today.date()==queryDate.date()) {
        queryLiveData(livedataUrl(app));
    }
}

function queryLiveData(url) {
 axios.get( url, { timeout: 11000 }).
            then(function (response) {
                app.localDueTrams = response.data.departures;
                app.noLiveResults = (app.localDueTrams.length==0);
                app.networkError = false;
                app.liveInProgress = false;
            }).
            catch(function (error) {
               app.networkError = true;
               app.liveInProgress = false;
               console.log(error);
            });
}

function getStationsFromServer(app) {
     axios
         .get(stationsUrl(app))
         .then(function (response) {
             app.networkError = false;
             app.proximityGroups = response.data.proximityGroups;
             app.stops = response.data.stations;
             app.ready=true;
         })
         .catch(function (error) {
             app.networkError = true;
             app.ready=true;
             console.log(error);
         });
 }

const app = new Vue({
        el: '#journeyplan',
        data () {
            return {
                ready: false,                   // ready to respond
                stops: [],                      // all stops
                proximityGroups: [],
                startStop: null,
                endStop: null,
                time: getCurrentTime(),
                date: getCurrentDate(),
                journeys: [],
                notes: [],
                buildNumber: '',
                feedinfo: [],
                localDueTrams: [],
                noResults: false,           // no routes found
                noLiveResults: false,       // no live data found
                searchInProgress: false,    // searching for routes
                liveInProgress: false,      // looking for live data
                networkError: false,        // network error on either query
                hasGeo: false,
                location: null,
                currentPage: 1,             // current page for due trams
                journeyFields: [
                    {key:'_showDetails',label:'', formatter: this.rowExpandedFormatter},
                    {key:'firstDepartureTime',label:'Depart', sortable:true, tdClass:'departTime'},
                    {key:'expectedArrivalTime',label:'Arrive', sortable:false, tdClass:'arriveTime'},
                    {key:'changeStations', label:'Change', tdClass:'changes', formatter: this.changesFormatter}
                    ],
                stageFields: [{key:'firstDepartureTime',label:'Time',tdClass:'departTime'},
                    {key:'action',label:'Action',tdClass:'action' },
                    {key:'actionStation.name',label:'Station', tdClass:'actionStation', formatter: this.stationFormatter},
                    {key:'platform.platformNumber', label:'Platform', tdClass:'platform'},
                    {key:'headSign', label:'Towards', tdClass: this.stageHeadsignClass },
                    {key:'routeName', label:'Line', tdClass: this.stageRowClass },
                    {key:'passedStops', label:'Stops', tdClass:'passedStops'}],
                departureFields: [
                    {key:'from', label:'From', tdClass:'departureDueFrom', sortable:true},
                    {key:'when', label:'Time', tdClass:'departureDueTime', sortable:true},
                    {key:'carriages', label:'', tdClass:'departureCarriages'},
                    {key:'status', label:'Status', tdClass:'departureStatus'},
                    {key:'destination', label:'Towards', tdClass:'departureTowards'}
                ]
            }
        },
        methods: {
            clearResults() {
                while(app.journeys.length>0) {
                    app.journeys.pop();
                }
                while(app.notes.length>0) {
                    app.notes.pop();
                }
                while(app.localDueTrams.length>0) {
                    app.localDueTrams.pop();
                }
                app.currentPage = 1;
            },
            plan(event){
                if (event!=null) {
                    event.preventDefault(); // stop page reload on form submission
                }
                app.searchInProgress = true;
                app.clearResults();
                app.ready = false;
                this.$nextTick(function () {
                    app.queryServer();
                });
            },
            earlier() {
                // TODO get max wait time from the server?
                var newTime = moment(app.time,"HH:mm").subtract(12, 'minutes');
                app.time = newTime.format("HH:mm");
                app.plan(null);
            },
            later() {
                const indexOfLast = app.journeys.length - 1;
                const lastJourney = app.journeys[indexOfLast];
                const lastDepartTime = lastJourney.firstDepartureTime;
                //const newTime = moment(lastDepartTime, "HH:mm");
                app.time = lastDepartTime;
                app.plan(null);

            },
            queryNearbyTrams() {
                app.liveInProgress = true;
                while(app.localDueTrams.length>0) {
                    app.localDueTrams.pop();
                }
                app.currentPage = 1;
                this.$nextTick(function () {
                    queryLiveData(livedataUrlFromLocation(this));
                });
            },
            queryServer() {
                var urlParams = { start: this.startStop, end: this.endStop, departureTime: this.time,
                    departureDate: this.date};
                if (this.startStop=='MyLocationPlaceholderId') {
                    const place = app.location;
                    urlParams.lat = place.coords.latitude;
                    urlParams.lon = place.coords.longitude;
                }
                axios.get('/api/journey', { params: urlParams, timeout: 11000}).
                    then(function (response) {
                        app.networkError = false;
                        app.journeys = app.journeys.concat(response.data.journeys);
                        app.noResults = app.journeys.length==0;
                        app.notes = app.notes.concat(response.data.notes);
                        app.getStations();
                        app.searchInProgress = false;
                        }).
                    catch(function (error) {
                        app.searchInProgress = false;
                        app.ready = true;
                        app.networkError = true;
                        console.log(error);
                    });
                displayLiveData(this);
            },
            getStations() {
                getStationsFromServer(this);
            },
            expandStages(row,index) {
                row._showDetails = !row._showDetails;
            },
            stageRowClass(value, key, item) {
                if (value && key === 'routeName') {
                    return item.displayClass;
                }
                return null;
            },
            stationFormatter(value, key, row) {
                var url = 'https://www.google.com/maps/search/?api=1&query='
                    + row.actionStation.latLong.lat + ',' + row.actionStation.latLong.lon;
                return `<a href='${url}' target="_blank">${row.actionStation.name}</a>`
            },
            changesFormatter(value, key, row) {
                if (value.length==0) {
                    return "Direct";
                }
                var result = "";
                value.forEach(change => {
                    if (result.length>0) result = result.concat(", ");
                    result = result.concat(change)});
                return result;
            },
            rowExpandedFormatter(value, key, row) {
                if (row._showDetails!=null && row._showDetails) {
                    return "&#8897;";
                } else {
                    return "&#8811;";
                }
            },
            stageHeadsignClass(value, key, item) {
                if (value === 'WalkingHeadSign') {
                    return 'HideWalkingHeadSign';
                }
                return "headsign";
            },
            setCookie() {
                var cookie = { 'visited' : true };
                var expiry = moment().add(100, 'days').toDate();
                this.$cookies.set("tramchesterVisited",cookie,"128d");
            },
            timeToNow() {
                app.time = getCurrentTime();
            },
            dateToNow() {
                app.date = getCurrentDate();
            },
            filterStops(group) {
                var result = [];
                app.stops.forEach(function(stop) { if (stop.proximityGroup.order===group.order) result.push(stop); } )
                return result;
            },
            filterEndStops(group) {
                var result = [];
                app.stops.forEach(function(stop) {
                    if (stop.proximityGroup.order===group.order && stop.id!==app.startStop) result.push(stop); } )
                return result;
            }
        }
        ,
        mounted () {
            var cookie = this.$cookies.get("tramchesterVisited");
            if (cookie==null) {
                this.$refs.cookieModal.show();
            }
            axios
                .get('/api/feedinfo')
                .then(function (response) {
                    app.networkError = false;
                    app.feedinfo = response.data;})
                .catch(function (error) {
                    this.networkError = true;
                    console.log(error);
                });
            axios
                .get('/api/version')
                .then(function (response) {
                    app.networkError = false;
                    app.buildNumber = response.data.buildNumber;})
                .catch(function (error) {
                    app.networkError = true;
                    console.log(error);
                });
            if (this.hasGeo) {
                navigator.geolocation.getCurrentPosition(pos => {
                      this.location = pos;
                      getStationsFromServer(this);
                }, err => {
                      this.location = null;
                      getStationsFromServer(this);
                })
            } else {
                getStationsFromServer(this);
            }
        },
        created() {
            if("geolocation" in navigator) {
                this.hasGeo = true;
            }
        },
        computed: {
            endProximityGroups: function () {
                // nearby not available for destinations yet...
                return this.proximityGroups.filter(group => group.name!=='Nearby');
            },
            // endStops: function () {
            //     return this.stops.filter(item => item.id!=this.startStop);
            // },
            havePos: function () {
                return this.hasGeo && (this.location!=null);
            },
            placeName: function () {
            }
        }

    })
