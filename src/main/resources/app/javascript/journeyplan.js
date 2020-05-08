
var moment = require('moment');
const axios = require('axios');
var _ = require('lodash');
var Vue = require('vue');
Vue.use(require('vue-cookies'));
Vue.use(require('bootstrap-vue'));

import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue/dist/bootstrap-vue.css'
import './../css/tramchester.css'

import Notes from "./components/Notes";
import Journeys from './components/Journeys';
import Footer from './components/Footer';
import LiveDepartures from './components/LiveDepatures'
import LocationSelection from './components/LocationSelection';

const dateFormat = "YYYY-MM-DD";
//const dateFormat = "DD-MM-YYYY";

function getCurrentTime() {
    return moment().local().format("HH:mm");
}

function getCurrentDate() {
    return moment().format(dateFormat)
}

function stationsUrl(base, app) {
    //var base = '/api/stations';
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
        return livedataUrlFromLocation(app)+'?querytime='+app.time;
    } else {
        return '/api/departures/station/'+app.startStop+'?querytime='+app.time;
    }
}

function displayLiveData(app) {
    var queryDate = moment(app.date, dateFormat);
    var today = moment();
    // check live data for today only - todo, into the API
    if (today.month()==queryDate.month()
        && today.year()==queryDate.year()
        && today.date()==queryDate.date()) {
        queryLiveData(livedataUrl(app));
    }
}

function queryLiveData(url) {
 axios.get( url, { timeout: 11000 }).
            then(function (response) {
                app.liveDepartureResponse = response.data;
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
         .get(stationsUrl('/api/stations', app))
         .then(function (response) {
             app.networkError = false;
             app.proximityGroups = response.data.proximityGroups;
             app.stops = response.data.stations;
             app.ready = true;
         })
         .catch(function (error) {
             app.networkError = true;
             app.ready = true;
             console.log(error);
         });
 }

 function updateStationsFromServer(app) {
    axios
    .get(stationsUrl('/api/stations/update', app))
        .then(function (response) {
            app.networkError = false;
            refreshStops(app, response.data);
            app.ready = true;
        })
        .catch(function (error) {
            app.networkError = true;
            app.ready = true;
            console.log(error);
        });
 }

 function refreshStops(app, updates) {
    var updatedStops = updates.stations;
    var updatedProxGroups = updates.proximityGroups;

    var updatedProxGroupNames = [];
    for(var i = 0; i< updatedProxGroups.length; i++) {
        updatedProxGroupNames.push(updatedProxGroups[i].name)
    }

    var updatedStopIds = [];
    for(var i = 0; i< updatedStops.length; i++) {
        updatedStopIds.push(updatedStops[i].id)
    }

    for(var i = 0; i< app.stops.length; i++) {
        var currentStop = app.stops[i]
        var currentProxGroup = currentStop.proximityGroup.name;
        if (updatedProxGroupNames.includes(currentProxGroup)) {
            app.stops[i].proximityGroup = { order:4, name:"All Stops" }
        }

        if (updatedStopIds.includes(currentStop.id)) {
            var indexIntoUpdated = updatedStopIds.indexOf(currentStop.id);
            app.stops[i].proximityGroup = updatedStops[indexIntoUpdated].proximityGroup;
        }
    }
 
 }

 var data = {
    ready: false,                   // ready to respond
    stops: [],                      // all stops
    proximityGroups: [],
    startStop: null,
    endStop: null,
    arriveBy: false,
    time: getCurrentTime(),
    date: getCurrentDate(),
    maxChanges: 8,                  // todo from server side
    journeyResponse: null,
    liveDepartureResponse: null,
    feedinfo: [],
    searchInProgress: false,    // searching for routes
    liveInProgress: false,      // looking for live data
    networkError: false,        // network error on either query
    hasGeo: false,
    location: null
}

var app = new Vue({
        el: '#journeyplan',
        data:  data,
        components: {
            'notes' : Notes,
            'journeys' : Journeys,
            'app-footer' : Footer,
            'live-departures' : LiveDepartures,
            'location-selection': LocationSelection
        },
        methods: {
            plan(event){
                if (event!=null) {
                    event.preventDefault(); // stop page reload on form submission
                }
                app.searchInProgress = true;
                app.ready = false;
                this.$nextTick(function () {
                    app.queryServer();
                });
            },
            changeTime(newTime) {
                app.time = newTime;
                app.plan(null);
            },
            networkErrorOccured() {
                app.networkError = true;
            },
            queryNearbyTrams() {
                app.liveInProgress = true;
                this.$nextTick(function () {
                    queryLiveData(livedataUrlFromLocation(this)+'?notes=1');
                });
            },
            queryServer() {
                var urlParams = { start: this.startStop, end: this.endStop, departureTime: this.time,
                    departureDate: this.date, arriveby: this.arriveBy, maxChanges: this.maxChanges};
                if (this.startStop=='MyLocationPlaceholderId' || this.endStop=='MyLocationPlaceholderId') {
                    const place = app.location;
                    urlParams.lat = place.coords.latitude;
                    urlParams.lon = place.coords.longitude;
                }
                axios.get('/api/journey', { params: urlParams, timeout: 11000}).
                    then(function (response) {
                        app.networkError = false;
                        app.journeyResponse = response.data;
                        app.updateStations();
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
            updateStations() {
                updateStationsFromServer(this);
            },
            setCookie() {
                var cookie = { 'visited' : true };
                this.$cookies.set("tramchesterVisited", cookie, "128d");
            },
            timeToNow() {
                app.time = getCurrentTime();
            },
            dateToNow() {
                app.date = getCurrentDate();
            },
            swap() {
                let temp = app.endStop;
                app.endStop = app.startStop;
                app.startStop = temp;
            }
        },
        mounted () {
            var cookie = this.$cookies.get("tramchesterVisited");
            if (cookie==null) {
                this.$refs.cookieModal.show();
            }
            axios.get('/api/feedinfo')
                .then(function (response) {
                    app.networkError = false;
                    app.feedinfo = response.data;})
                .catch(function (error) {
                    this.networkError = true;
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
                return this.proximityGroups; //.filter(group => group.name!=='Nearby');
            },
            havePos: function () {
                return this.hasGeo && (this.location!=null);
            }
        }
    })
