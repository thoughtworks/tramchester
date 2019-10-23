
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

function getStationsFromServer(app) {
     axios
         .get(stationsUrl(app))
         .then(function (response) {
             app.networkError = false;
             var groupSeen = [];
             // respect way vue bindings work, can't just assign/overwrite existing list
             var changes = response.data.stations.filter(station =>
                 station.proximityGroup.order != app.stopToProxGroup.get(station.id) );
             changes.forEach(function(change) {
                 app.stopToProxGroup.set(change.id, change.proximityGroup.order);
                 if (!groupSeen.includes(change.proximityGroup.order)) {
                     app.proximityGroups.push(change.proximityGroup);
                     groupSeen.push(change.proximityGroup.order);
                 }
             });

             app.stops = app.stops.filter(stop =>
                 stop.proximityGroup.order === app.stopToProxGroup.get(stop.id) ); // keep unchanged

             changes.forEach(function(change) {
                 app.stops.push(change);
             });
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
                ready: false,
                stops: [],
                stopToProxGroup: new Map(),
                proximityGroups: [],
                startStop: null,
                endStop: null,
                time: getCurrentTime(),
                date: getCurrentDate(),
                journeys: [],
                notes: [],
                buildNumber: '',
                feedinfo: [],
                noResults: false,
                searchInProgress: false,
                networkError: false,
                hasGeo: false,
                location: null,
                journeyFields: [
                    {key:'_showDetails',label:'', formatter: this.rowExpandedFormatter},
                    {key:'firstDepartureTime',label:'Depart', sortable:true, tdClass:'departTime'},
                    {key:'expectedArrivalTime',label:'Arrive', sortable:false, tdClass:'arriveTime'},
                    {key:'changeStations', label:'Change', tdClass:'changes', formatter: this.changesFormatter},
                    {key:'dueTram', label:'Due', tdClass: 'dueTram'}],
                stageFields: [{key:'firstDepartureTime',label:'Time',tdClass:'departTime'},
                    {key:'action',label:'Action',tdClass:'action' },
                    {key:'actionStation.name',label:'Station', tdClass:'actionStation', formatter: this.stationFormatter},
                    {key:'platform.platformNumber', label:'Platform', tdClass:'platform'},
                    {key:'headSign', label:'Towards', tdClass: this.stageHeadsignClass },
                    {key:'routeName', label:'Line', tdClass: this.stageRowClass },
                    {key:'passedStops', label:'Stops', tdClass:'passedStops'}]
            }
        },
        methods: {
            clearJourneysAndNotes() {
                while(app.journeys.length>0) {
                    app.journeys.pop();
                }
                 while(app.notes.length>0) {
                    app.notes.pop();
                }
            },
            plan(event){
                event.preventDefault(); // stop page reload on form submission
                app.searchInProgress = true;
                app.clearJourneysAndNotes();
                app.ready = false;
                this.$nextTick(function () {
                    app.queryServer();
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
                axios.get('/api/journey', { params: urlParams, timeout: 11000
                    }).then(function (response) {
                        app.networkError = false;
                        app.searchInProgress = false;
                        app.journeys = app.journeys.concat(response.data.journeys);
                        app.noResults = app.journeys.length==0;
                        app.notes = app.notes.concat(response.data.notes);
                        app.getStations(); // recent stations will have changed
                    }).catch(function (error) {
                        app.searchInProgress = false;
                        app.networkError = true;
                        console.log(error);
                });
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
                this.$cookies.set("tramchesterVisited",cookie,"1d");
            },
            timeToNow() {
                app.time = getCurrentTime();
            },
            dateToNow() {
                app.date = getCurrentDate();
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
            endStops: function () {
                return this.stops.filter(item => item.id!=this.startStop);
            },
            havePos: function () {
                return this.hasGeo && (this.location!=null);
            }
        }

    })
