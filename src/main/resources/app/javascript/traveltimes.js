
const axios = require('axios');
var _ = require('lodash');

var Vue = require('vue');
Vue.use(require('vue-cookies'));
Vue.use(require('bootstrap-vue'));

var L = require('leaflet');
require('leaflet-arrowheads')

import 'bootstrap/dist/css/bootstrap.css';
import 'bootstrap-vue/dist/bootstrap-vue.css';
import 'leaflet/dist/leaflet.css'
import './../css/tramchester.css'

L.Icon.Default.imagePath = '/app/dist/images/';
require("leaflet/dist/images/marker-icon-2x.png");
require("leaflet/dist/images/marker-shadow.png");

import Footer from './components/Footer';
import { map } from 'lodash';

var width = 300;
var height = 300;
var margin = 60;

// function addStations() {
//     mapApp.routes.forEach(route => {
//         var stationIcon = L.divIcon({className: 'station-icon '+route.displayClass, iconSize:[12,12]});
//         addStationsForRoute(route, stationIcon);
//     })
// }

function addPostcodes() {
    var postcodeLayerGroup = L.featureGroup();

    mapApp.postcodes.forEach(postcode => {
        var lat = postcode.latLong.lat;
        var lon = postcode.latLong.lon;
        var marker = new L.circleMarker(L.latLng(lat,lon), {  radius: 1, title: postcode.name })
        postcodeLayerGroup.addLayer(marker);
    });

    postcodeLayerGroup.addTo(mapApp.map);
}

function addRoutes() {
    var routeLayerGroup = L.featureGroup();

    mapApp.routes.forEach(route => {
        var steps = [];
        route.stations.forEach(station => {
            steps.push([station.latLong.lat, station.latLong.lon]);
        })
        var line = L.polyline(steps, {className: route.displayClass});
        routeLayerGroup.addLayer(line);
    })

    routeLayerGroup.addTo(mapApp.map);
}

// function addTrams() {
//     var tramIcon =  L.divIcon({className: 'arrow-up', html: 'transform: rotate(20deg);'});
   
//     mapApp.tramLayerGroup.clearLayers();
//     mapApp.positionsList.forEach(position => {
//         var latBegin = position.first.latLong.lat;
//         var lonBegin = position.first.latLong.lon;
//         var latNext = position.second.latLong.lat;
//         var lonNext = position.second.latLong.lon;
        
//         // unit vector based on cost between the stations
//         var vectorLat = (latNext-latBegin)/position.cost;
//         var vectorLon = (lonNext-lonBegin)/position.cost;

//         position.trams.forEach(tram => {
//             var dist = (position.cost-tram.wait); // approx current dist travelled based on wait at next station
//             var latCurrent = latBegin + ( dist * vectorLat );
//             var lonCurrent = lonBegin + ( dist * vectorLon );
//             if (tram.wait>0) {
//                 var latTorwards = latCurrent + (vectorLat*0.8);
//                 var lonTowards = lonCurrent + (vectorLon*0.8);
//                 var linePoints = [ [latCurrent,lonCurrent] , [latTorwards,lonTowards] ];

//                 var line = L.polyline(linePoints, { color: 'black', opacity: 0.6, weight: 4, pane: mapApp.tramPane}).
//                     arrowheads({ opacity: 1, fill: false, size: '8px', pane: mapApp.tramPane, yaw: 90 });
//                 line.bindTooltip(getTramTitle(tram, position));
//                 mapApp.tramLayerGroup.addLayer(line);
//             } else {
//                 var circle = L.circle([latCurrent, lonCurrent], {radius: 50, color: 'black', weight: 2, pane: mapApp.tramPane});
//                 circle.bindTooltip(getTramTitle(tram, position));
//                 mapApp.tramLayerGroup.addLayer(circle);
//             }

            
//         })
//         mapApp.tramLayerGroup.addTo(mapApp.map);
//     });
// }


function findAndSetMapBounds() {
    let minLat = 1000;
    let maxLat = -1000;
    let minLon = 1000;
    let maxLon = -1000;
    mapApp.routes.forEach(route => {
        route.stations.forEach(position => {
            var lat = position.latLong.lat;
            if (lat < minLat) {
                minLat = lat;
            }
            else if (lat > maxLat) {
                maxLat = lat;
            }
            var lon = position.latLong.lon;
            if (lon < minLon) {
                minLon = lon;
            }
            else if (lon > maxLon) {
                maxLon = lon;
            }
        });
    })

    var corner1 = L.latLng(minLat, minLon);
    var corner2 = L.latLng(maxLat, maxLon);
    var bounds = L.latLngBounds(corner1, corner2);
    mapApp.map.fitBounds(bounds);
}

var mapApp = new Vue({
    el: '#tramMap',
    components: {
        'app-footer' : Footer
    },
    data() {
        return {
            map: null,
            networkError: false,
            routes: [],
            postcodes: [],
            postcodeLayerGroup: null,
            feedinfo: [],
            // tramPane: null
        }
    },
    methods: {
        networkErrorOccured() {
            app.networkError = true;
        },
        draw() {
            findAndSetMapBounds();
            // mapApp.tramPane = mapApp.map.createPane("tramPane");
            // mapApp.tramPane.style.zIndex = 610; // above marker plane, below popups and tooltips
            // mapApp.tramLayerGroup = L.featureGroup();
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(mapApp.map);
            addRoutes();
        }
    },
    mounted () {
        this.map = L.map('leafletMap');

        axios.get('/api/feedinfo')
            .then(function (response) {
                mapApp.networkError = false;
                mapApp.feedinfo = response.data;
            }).catch(function (error) {
                mapApp.networkError = true;
                console.log(error);
            });
        axios.get("/api/routes")
            .then(function (response) {
                mapApp.networkError = false;
                mapApp.routes = response.data;
                mapApp.draw();
            }).catch(function (error){
                mapApp.networkError = true;
                console.log(error);
            });
        axios.get('/api/postcodes')
            .then(function (response) {
                mapApp.networkError = false;
                mapApp.postcodes = response.data;
                addPostcodes();
            }).catch(function (error) {
                mapApp.networkError = true;
                console.log(error);
            });
    }, 
    computed: {
        havePos: function () {
            return false; // needed for display in footer
        }
    }
});


