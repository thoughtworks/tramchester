
const axios = require('axios');
var Vue = require('vue');
Vue.use(require('vue-cookies'));
Vue.use(require('bootstrap-vue'));

require('file-loader?name=[name].[ext]!../index.html');

import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue/dist/bootstrap-vue.css'
import './../css/tramchester.css'

import Notes from "./components/Notes";
import Journeys from './components/Journeys';
import Footer from './components/Footer';
import LiveDepartures from './components/LiveDepatures'
import LocationSelection from './components/LocationSelection';
import Closures from './components/Closures'

function getNow() {
    return new Date();
}

function getCurrentTime() {
    return getNow().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
}

function getCurrentDate() {
    const now = new Date().toISOString();
    return now.substr(0,  now.indexOf("T")); // iso-8601 date part only as YYYY-MM-DD
}

function livedataUrlFromLocation() {
    var place = app.location; // should not have location place holder without a valid location
    return '/api/departures/' + place.coords.latitude + '/' + place.coords.longitude;
}

function livedataUrl() {
    if (app.startStop==null || app.startStop==='MyLocationPlaceholderId') {
        return livedataUrlFromLocation(app)+'?querytime='+app.time;
    } else {
        return '/api/departures/station/'+app.startStop+'?querytime='+app.time;
    }
}

function busEnabled(scope) {
    return scope.modes.includes('Bus');
}

function displayLiveData(app) {
    if (busEnabled(app)) {
        // only live data for trams
        // TODO nearby when buses enabled?
        if (!app.startStop.startsWith('9400ZZ')) {
            app.liveDepartureResponse = null;
            return; 
        }
    }
    var queryDate = new Date(app.date); 
    var today = getNow();
    // check live data for today only 
    if (today.getMonth()==queryDate.getMonth()
        && today.getYear()==queryDate.getYear()
        && today.getDate()==queryDate.getDate()) {
        queryLiveData(livedataUrl());
    } else {
        app.liveDepartureResponse = null;
    }

}

function queryLiveData(url) {
 axios.get( url, { timeout: 11000 }).
            then(function (response) {
                app.liveDepartureResponse = addParsedDatesToLive(response.data);
                app.networkError = false;
                app.liveInProgress = false;
            }).
            catch(function (error) {
                app.liveInProgress = false;
                reportError(error);
            });
}

function getFeedinfo(app) {
    axios.get('/api/datainfo')
        .then(function (response) {
            app.networkError = false;
            app.feedinfo = response.data;
        })
        .catch(function (error) {
            reportError(error);
        });
}

function getTransportModesThenStations(app) {
    axios.get('/api/version/modes')
        .then(function (response) {
            app.networkError = false;
            app.modes = response.data.modes;
            app.postcodesEnabled = response.data.postcodesEnabled;
            app.numberJourneysToDisplay = response.data.numberJourneysToDisplay;
            getStations(app);
        })
        .catch(function (error) {
            reportError(error);
        });
}

function getStations(app) {
    app.location = null;
    if (app.hasGeo) {
        navigator.geolocation.getCurrentPosition(pos => {
            app.location = pos;
            getStationsFromServer(app);
        }, err => {
            console.log("Location disabled: " + err.message);
            getStationsFromServer(app);
        });
    } else {
        console.log("Location disabled");
        getStationsFromServer(app);
    }
}

async function getStationsFromServer(app) {

    var gets = [];
    if (app.postcodesEnabled) {
        gets.push(axios.get("/api/postcodes", { timeout: 30000}));
    }
    app.modes.forEach(mode => {
        gets.push(axios.get('/api/stations/mode/'+mode));
    });

    if (gets.length==0) {
        console.error("No modes?");
    }

    await Promise.allSettled(gets).then(function(results) {
        app.stops.allStops = new Map();
        results.forEach(result => {
            var receivedStops = result.value.data;
            receivedStops.forEach(stop => app.stops.allStops.set(stop.id, stop))
        });
        app.stops.allStops = Object.freeze(app.stops.allStops); //performance, still needed?
        app.ready = true;
    });

    getRecentAndNearest(app);
    app.networkError = false;
 }

async function getRecentAndNearest(app) {
    await axios
        .get('/api/stations/recent')
        .then(function (response) {
            app.networkError = false;
            app.stops.recentStops = response.data;
        })
        .catch(function (error) {
            reportError(error);
        });
    if (app.hasGeo && app.location!=null) {
        var place = app.location;
        const url = '/api/stations/near/?lat=' + place.coords.latitude + '&lon=' + place.coords.longitude;
        await axios
            .get(url)
            .then(function (response) {
                app.networkError = false;
                app.stops.nearestStops = response.data;
            })
            .catch(function (error) {
                reportError(error);
            });
    }
}


// json parses dates to string
function addParsedDatesToJourney(journeysArray) {
    journeysArray.forEach(item => {
        item.journey.queryDateAsDate = new Date(item.journey.queryDate);
        item.journey.firstDepartureTimeAsDate = new Date(item.journey.firstDepartureTime);
        item.journey.expectedArrivalTimeAsDate = new Date(item.journey.expectedArrivalTime);
    })
    return journeysArray;
}

// json parses dates to string
function addParsedDatesToLive(liveData) {
    liveData.departures.forEach(item => {
        item.dueTimeAsDate = new Date(item.dueTime);
    })
    return liveData;
}

 function queryServerForJourneys(app, startStop, endStop, time, date, arriveBy, changes) {
    var urlParams = {
        start: startStop, end: endStop, departureTime: time, departureDate: date, 
        arriveby: arriveBy, maxChanges: changes
    };
    if (startStop == 'MyLocationPlaceholderId' || endStop == 'MyLocationPlaceholderId') {
        const place = app.location;
        urlParams.lat = place.coords.latitude;
        urlParams.lon = place.coords.longitude;
    }
    axios.get('/api/journey/', { params: urlParams, timeout: 60000 }).
        then(function (response) {
            app.networkError = false;
            app.journeys = addParsedDatesToJourney(response.data.journeys);
            getRecentAndNearest(app);
            app.searchInProgress = false;
            app.ready = true;
        }).
        catch(function (error) {
            app.ready = true;
            app.searchInProgress = false;
            reportError(error);
        });
}

 function reportError(error) {
    app.networkError = true;
    console.log(error.message);
    console.log("File: " + error.fileName);
    console.log("Line:" + error.lineNumber);
    if (error.request!=null) {
        console.log("URL: " + error.request.responseURL);
    }

 }

 var data = {
    ready: false,                   // ready to respond
    stops: {
        allStops: null,        // (station) id->station
        nearestStops: [],
        recentStops: []
    },
    startStop: null,
    endStop: null,
    arriveBy: false,
    time: getCurrentTime(),
    date: getCurrentDate(),
    maxChanges: 3,                  // todo from server side
    journeys: null,
    liveDepartureResponse: null,
    feedinfo: [],
    modes: [],
    numberJourneysToDisplay: 0,
    searchInProgress: false,    // searching for routes
    liveInProgress: false,      // looking for live data
    networkError: false,        // network error on either query
    hasGeo: false,
    location: null,
    postcodesEnabled: false
}

var app = new Vue({
        el: '#journeyplan',
        data:  data,
        components: {
            'notes' : Notes,
            'journeys' : Journeys,
            'app-footer' : Footer,
            'live-departures' : LiveDepartures,
            'location-selection': LocationSelection,
            'closures' : Closures
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
                    queryLiveData(livedataUrlFromLocation()+'?notes=1');
                });
            },
            queryServer() {
                queryServerForJourneys(app, this.startStop, this.endStop, this.time,
                    this.date, this.arriveBy, this.maxChanges);
                displayLiveData(app);
            },
            setCookie() {
                var cookie = { 'visited' : true };
                this.$cookies.set("tramchesterVisited", cookie, "128d", "/", null, false, "Strict");
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
            getFeedinfo(this);
            getTransportModesThenStations(this);
        },
        created() {
            if("geolocation" in navigator) {
                this.hasGeo = true;
            }
        },
        computed: {
            havePos: function () {
                return this.hasGeo && (this.location!=null);
            },
            busEnabled: function () {
                return busEnabled(this);
            }
        }
    })



