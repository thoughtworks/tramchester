
const axios = require('axios');
var _ = require('lodash');

var Vue = require('vue');
Vue.use(require('vue-cookies'));
Vue.use(require('bootstrap-vue'));

var L = require('leaflet');

import 'bootstrap/dist/css/bootstrap.css';
import 'bootstrap-vue/dist/bootstrap-vue.css';
import 'leaflet/dist/leaflet.css'
import './../css/tramchester.css'

L.Icon.Default.imagePath = '/app/dist/images/';
require("leaflet/dist/images/marker-icon-2x.png");
require("leaflet/dist/images/marker-shadow.png");

import Footer from './components/Footer';

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
    mapApp.routes.forEach(route => {
        var stationIcon = L.divIcon({className: 'station-icon '+route.displayClass, iconSize:[12,12]});
        addStationsForRoute(route, stationIcon);
    })
}

function addStationsForRoute(route, stationIcon) {
    var stationLayerGroup = L.layerGroup();
    var stationRender = L.canvas({ padding: 0.5 }); // todo needed??

    route.stations.forEach(station => {
        if (station.tram) {
            var lat = station.latLong.lat;
            var lon = station.latLong.lon;
            var marker = new L.marker(L.latLng(lat,lon), { title: station.name, icon: stationIcon })
            stationLayerGroup.addLayer(marker);
        }
    });
    stationLayerGroup.addTo(mapApp.map);
}

function addRoutes() {
    var routeLayerGroup = L.layerGroup();

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

function refreshTrams() {
    axios.get('/api/positions')
        .then(function (response) {
            mapApp.networkError = false;
            mapApp.positionsList = response.data.positionsList;
            addTrams();
        }).catch(function (error) {
            mapApp.networkError = true;
            console.log(error);
        });
}

function addTrams() {
    var tramIcon =  L.divIcon({className: 'tram-icon', iconSize:[8,8]});
   
    mapApp.tramLayerGroup.clearLayers();
    mapApp.positionsList.forEach(position => {
        var latA = position.first.latLong.lat;
        var lonA = position.first.latLong.lon;
        var latB = position.second.latLong.lat;
        var lonB = position.second.latLong.lon;
        
        var vectorLat = (latB-latA)/position.cost;
        var vectorLon = (lonB-lonA)/position.cost;

        position.trams.forEach(tram => {
            var dist = (position.cost-tram.wait);
            var lat = latA + ( dist * vectorLat );
            var lon = lonA + ( dist * vectorLon );
            var marker = new L.marker(L.latLng(lat,lon), { title: getTramTitle(tram, position) , icon: tramIcon }); 
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
            positionsList: null,
            networkError: false,
            routes: [],
            tramLayerGroup: null,
            feedinfo: []
        }
    },
    methods: {
        networkErrorOccured() {
            app.networkError = true;
        },
        draw() {
            findAndSetMapBounds();
            mapApp.tramLayerGroup = L.layerGroup();
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(mapApp.map);
            addRoutes();
            addStations();
            refreshTrams();
            setInterval(function() {
                refreshTrams();
            }, 10 * 1000);
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
                mapApp.routes = response.data;
                mapApp.draw();
            }).catch(function (error){
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


