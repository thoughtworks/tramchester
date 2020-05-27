
const axios = require('axios');
var _ = require('lodash');

var Vue = require('vue');
Vue.use(require('vue-cookies'));
Vue.use(require('bootstrap-vue'));

var d3 = require("d3");
var L = require('leaflet');

import 'bootstrap/dist/css/bootstrap.css';
import 'bootstrap-vue/dist/bootstrap-vue.css';
import 'leaflet/dist/leaflet.css'
import './../css/tramchester.css'

L.Icon.Default.imagePath = '/app/dist/images/';
require("leaflet/dist/images/marker-icon-2x.png");
require("leaflet/dist/images/marker-shadow.png");

var width = 800;
var height = 800;
var margin = 60;

function textFor(trams) {
    var text = "";
    for (var i = 0; i < trams.length; i++) {
        if (i>0) text = text + "\n";
        text = text + trams[i].destination;
    }
    return text;
}

function addStations() {
    var stationIcon = L.divIcon({className: 'station-div-icon'});
    mapApp.uniqueStations.forEach(station => {
        var lat = station.latLong.lat;
        var lon = station.latLong.lon;
        new L.marker(L.latLng(lat,lon), { title: station.name, icon: stationIcon }).addTo(mapApp.map);
    });
}

function addTrams() {
    var tramIcon =  L.divIcon({className: 'tram-div-icon'});
   
    mapApp.positionsList.forEach(position => {
        var latA = position.first.latLong.lat;
        var lonA = position.first.latLong.lon;
        var latB = position.second.latLong.lat;
        var lonB = position.second.latLong.lon;
        
        var deltaLat = (latB-latA)/position.cost;
        var deltaLon = (lonB-lonA)/position.cost;

        position.trams.forEach(tram => {
            var dist = (position.cost-tram.wait);
            var lat = latA + ( dist * deltaLat );
            var lon = lonA + ( dist * deltaLon );
            var marker = new L.marker(L.latLng(lat,lon), { title: getTramTitle(tram, position) , icon: tramIcon }); //.addTo(mapApp.map);
            mapApp.tramLayerGroup.addLayer(marker);
        })
        mapApp.tramLayerGroup.addTo(mapApp.map);
    });
}

function getTramTitle(tram, position) {
    if (tram.status==='Arrived') {
        return tram.destination + ' tram at ' + position.second.name;
    } else {
        return tram.destination + ' tram ' + tram.status + ' at ' + position.second.name + ' in ' + tram.wait;
    }
}

function addRoutes() {
    mapApp.routes.forEach(route => {
        var steps = [];
        route.stations.forEach(station => {
            steps.push([station.latLong.lat, station.latLong.lon]);
        })
        L.polyline(steps, {className: route.displayClass}).addTo(mapApp.map);
    })
}

function findAndSetMapBounds() {
    let minLat = 1000;
    let maxLat = -1000;
    let minLon = 1000;
    let maxLon = -1000;
    mapApp.uniqueStations.forEach(position => {
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
    var corner1 = L.latLng(minLat, minLon);
    var corner2 = L.latLng(maxLat, maxLon);
    var bounds = L.latLngBounds(corner1, corner2);
    mapApp.map.fitBounds(bounds);
}

var mapApp = new Vue({
    el: '#tramMap',
    data() {
        return {
            map: null,
            positionsList: null,
            uniqueStations: [],
            networkError: false,
            routes: [],
            tramLayerGroup: null
        }
    },
    methods: {
        createStationList() {
            // unique list of stations
            var ids = [];
            mapApp.routes.forEach(route => {
                route.stations.forEach(station => {
                    if (ids.indexOf(station.id)<0) {
                        ids.push(station.id);
                        mapApp.uniqueStations.push(station);
                    }
                });
            });
        },
        draw() {
            findAndSetMapBounds();
            mapApp.tramLayerGroup = L.layerGroup();
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(mapApp.map);
            addStations();
            addRoutes();
        }
    },
    mounted () {
        axios.get("/api/routes")
            .then(function (response) {
                mapApp.routes = response.data;
                mapApp.createStationList();
                mapApp.draw();
            }).catch(function (error){
                mapApp.networkError = true;
                console.log(error);
            });
        axios.get('/api/positions')
            .then(function (response) {
                mapApp.networkError = false;
                mapApp.positionsList = response.data.positionsList;
                addTrams();
            }).catch(function (error) {
                mapApp.networkError = true;
                console.log(error);
            });

        this.map = L.map('leafletMap');

    }
});

