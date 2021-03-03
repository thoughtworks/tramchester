
const axios = require('axios');

var Vue = require('vue');
Vue.use(require('bootstrap-vue'));

var L = require('leaflet');

require('file-loader?name=[name].[ext]!../links.html');

import 'bootstrap/dist/css/bootstrap.css';
import 'bootstrap-vue/dist/bootstrap-vue.css';
import 'leaflet/dist/leaflet.css'
import './../css/tramchester.css'

L.Icon.Default.imagePath = '/app/dist/images/';
require("leaflet/dist/images/marker-icon-2x.png");
require("leaflet/dist/images/marker-shadow.png");

import Footer from './components/Footer';

function updateBounds(position, minLat, maxLat, minLon, maxLon) {
    if (position.latLong.valid) {
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
    }
    return { minLat, maxLat, minLon, maxLon };
}

function addStationToMap(station, stationLayerGroup, displayed) {
    if (displayed.includes(station.id)) {
        return;
    }

    var lat = station.latLong.lat;
    var lon = station.latLong.lon;
    var marker = new L.circleMarker(L.latLng(lat, lon), { title: station.name, radius: 1 });
    marker.bindTooltip(station.name + "<br> '" + station.id + "' (" + station.transportModes + ")");
    displayed.push(station.id);
    stationLayerGroup.addLayer(marker);
}

var mapApp = new Vue({
    el: '#routeMap',
    components: {
        'app-footer' : Footer
    },
    data() {
        return {
            map: null,
            networkError: false,
            links: [],
            feedinfo: [],
        }
    },
    methods: {
        networkErrorOccured() {
            app.networkError = true;
        },
        addStations: function(map, links) {
            var displayed = [];

            var stationLayerGroup = L.layerGroup();
            links.forEach(link => {
                addStationToMap(link.begin, stationLayerGroup, displayed);
                addStationToMap(link.end, stationLayerGroup, displayed);
            })
            stationLayerGroup.addTo(map);
        },

        addLinks: function(map, links) {
            var routeLayerGroup = L.layerGroup();
        
            links.forEach(link => {
                var steps = [];
                steps.push([link.begin.latLong.lat, link.begin.latLong.lon]);
                steps.push([link.end.latLong.lat, link.end.latLong.lon]);

                var line = L.polyline(steps); 
                // line.bindTooltip(route.routeName + "<br>" 
                //     + "'" + route.id + "' (" + route.transportMode+")");
                // line.setStyle({className: this.classForRoute(route), weight: 6});
                // line.on({
                //     mouseover: this.highlightRoute,
                //     mouseout: this.unhighlightRoute
                // });
                routeLayerGroup.addLayer(line);
            })
        
            // faster to add this way for larger numbers of lines/points
            routeLayerGroup.addTo(map);
        }, 
        findAndSetMapBounds: function(map, links) {
            let minLat = 1000;
            let maxLat = -1000;
            let minLon = 1000;
            let maxLon = -1000;
            links.forEach(link => {
                ({ minLat, maxLat, minLon, maxLon } = updateBounds(link.begin, minLat, maxLat, minLon, maxLon));
                ({ minLat, maxLat, minLon, maxLon } = updateBounds(link.end, minLat, maxLat, minLon, maxLon));
            });
        
            var corner1 = L.latLng(minLat, minLon);
            var corner2 = L.latLng(maxLat, maxLon);
            var bounds = L.latLngBounds(corner1, corner2);
            map.fitBounds(bounds);
        },
        draw() {
            mapApp.findAndSetMapBounds(mapApp.map, mapApp.links);
            
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(mapApp.map);

            mapApp.addLinks(mapApp.map, mapApp.links);
            mapApp.addStations(mapApp.map, mapApp.links);
        }
    },
    mounted () {
        this.map = L.map('leafletMap');

        axios.get('/api/datainfo')
            .then(function (response) {
                mapApp.networkError = false;
                mapApp.feedinfo = response.data;
            }).catch(function (error) {
                mapApp.networkError = true;
                console.log(error);
            });

        axios.get("/api/links/all")
            .then(function (response) {
                mapApp.networkError = false;
                mapApp.links = response.data;
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



