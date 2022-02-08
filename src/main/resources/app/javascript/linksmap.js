
const axios = require('axios');

var Vue = require('vue');
Vue.use(require('bootstrap-vue'));

var L = require('leaflet');
import 'leaflet-arrowheads'

require('file-loader?name=[name].[ext]!../links.html');

import 'bootstrap/dist/css/bootstrap.css';
import 'bootstrap-vue/dist/bootstrap-vue.css';
import 'leaflet/dist/leaflet.css'
import './../css/tramchester.css'

L.Icon.Default.imagePath = '/app/dist/images/';
require("leaflet/dist/images/marker-icon-2x.png");
require("leaflet/dist/images/marker-shadow.png");

import Footer from './components/Footer';

function getColourFor(station) {
    const modes = station.transportModes;
    if (modes.length>1) {
        return "purple";
    }
    const mode = modes[0];
    switch(mode) {
        case "Bus": return "Green";
        case "Train": return "DarkBlue";
        case "Tram": return "LightBlue";
        default: return "Orange";
    }
}

function addStationToMap(station, stationLayerGroup, isInterchange) {
    const lat = station.latLong.lat;
    const lon = station.latLong.lon;
    const colour = getColourFor(station);

    var marker;
    if (isInterchange) {
        // todo really want a different kind of marker here
        marker = new L.circleMarker(L.latLng(lat, lon), { title: station.name, radius: 3, color: colour });
    } else {
        marker = new L.circleMarker(L.latLng(lat, lon), { title: station.name, radius: 1, color: colour });
    }
    var stationText = station.name + "<br> '" + station.id + "' (" + station.transportModes + ")";
    if (isInterchange) {
        stationText = stationText + "<br>interchange"
    }
    if (station.isMarkedInterchange) {
        stationText = stationText + "<br>marked interchange at source"
    }
    marker.bindTooltip(stationText);

    station.platforms.forEach(platform => addPlatformsToMap(platform, stationLayerGroup));

    stationLayerGroup.addLayer(marker);
}

function addPlatformsToMap(platform, stationLayerGroup) {
    const lat = platform.latLong.lat;
    const lon = platform.latLong.lon;

    var marker = new L.circleMarker(L.latLng(lat, lon), { title: platform.name, radius: 1, color: "black" });
    marker.bindTooltip("Platform " +platform.id+ "<br>Name " + platform.name);
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
            neighbours: [],
            quadrants: [],
            feedinfo: [],
            areas: [],
            stations: [],
            interchanges: [], // list of station id's
            bounds: null
        }
    },
    methods: {
        networkErrorOccured() {
            app.networkError = true;
        },
        addStations: function(map, stations, interchanges) {
            var stationLayerGroup = L.layerGroup();
            stations.forEach(station => {
                const isInterchange = interchanges.includes(station.id);
                addStationToMap(station, stationLayerGroup, isInterchange);
            })
            stationLayerGroup.addTo(map);
        },
        addAreas: function(map, areas) {
            var areaLayerGroup = L.layerGroup();
            
            areas.forEach(area => {
                const areaId = area.areaId;
                const boundary = area.points;
                var points = [];
                boundary.forEach(latLong => points.push([latLong.lat, latLong.lon]));
                var polygon = L.polygon(points, { stroke: true, weight: 1, fill: true,  fillOpacity: 0.5, color: "purple"});
                polygon.bindTooltip("area " + areaId + "<br> " + area.areaName + "<br>" + area.type);
                areaLayerGroup.addLayer(polygon);
            })

            areaLayerGroup.addTo(map);
        },
        addGroups: function(map, groups) {
            var groupLayer = L.layerGroup();
            groups.forEach(group => {
                var steps = [];
                group.contained.forEach(station => {
                    steps.push([station.latLong.lat, station.latLong.lon]);
                })
                var first = group.contained[0];
                steps.push([first.latLong.lat, first.latLong.lon]);
                var shape = L.polygon(steps);
                shape.setStyle({color: "pink", opacity: 0.7, fill: true, fillOpacity: 0.5});
                groupLayer.addLayer(shape);
            })
            groupLayer.addTo(map);
        },
        addQuadrants: function(map, quadrants) {
            var quadrantLayer = L.layerGroup();
            quadrants.forEach(quadrant => {
                var bottomLeft = quadrant.bottomLeft;
                var topRight = quadrant.topRight;
                var bounds = [ [bottomLeft.lat, bottomLeft.lon], [topRight.lat, topRight.lon] ];
                var box = L.rectangle(bounds, { color: "black", stroke: false });
                quadrantLayer.addLayer(box);
            });
            quadrantLayer.addTo(map);
        },
        addBounds: function(map, bounds) {
            var boundsLayer = L.layerGroup();
            var bottomLeft = bounds.bottomLeft;
            var topRight = bounds.topRight;
            var bounds = [ [bottomLeft.lat, bottomLeft.lon], [topRight.lat, topRight.lon] ];
            var box = L.rectangle(bounds, { color: "blue", stroke: true, weight: 1, fill: false });
            boundsLayer.addLayer(box);
            boundsLayer.addTo(map);
        },
        addLinks: function(map, links) {
            var linkLayerGroup = L.layerGroup();
            
            links.forEach(link => {
                var steps = [];
                steps.push([link.begin.latLong.lat, link.begin.latLong.lon]);
                steps.push([link.end.latLong.lat, link.end.latLong.lon]);
                var line = L.polyline(steps); // hurts performance .arrowheads({ size: '5px', frequency: 'endonly' });
                line.bindTooltip("Link between " + link.begin.name + " and " + link.end.name + "<br> " + link.distanceInMeters + "m");
                line.setStyle({color: "yellow", opacity: 0.6});
                linkLayerGroup.addLayer(line);
            });

            linkLayerGroup.addTo(map);
        },
        findAndSetMapBounds: function(map, bounds) {
            var bottomLeft = bounds.bottomLeft;
            var topRight = bounds.topRight;
        
            var corner1 = L.latLng(bottomLeft.lat, bottomLeft.lon);
            var corner2 = L.latLng(topRight.lat, topRight.lon);
            var bounds = L.latLngBounds(corner1, corner2);
            map.fitBounds(bounds);
        },
        draw() {
            const map = mapApp.map;
            mapApp.findAndSetMapBounds(map, mapApp.bounds);
            
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(map);

            mapApp.addBounds(map, mapApp.bounds);
            mapApp.addQuadrants(map, mapApp.quadrants);
            mapApp.addAreas(map, mapApp.areas);
            mapApp.addLinks(map, mapApp.neighbours);
            mapApp.addStations(map, mapApp.stations, mapApp.interchanges);
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

        axios.all([
            axios.get("/api/geo/neighbours"),
            axios.get("/api/geo/quadrants"),
            axios.get("/api/geo/bounds"),
            axios.get("/api/geo/areas"),
            axios.get("/api/stations/all"),
            axios.get("/api/interchanges/all")
        ]).then(axios.spread((neighboursResp, quadResp, boundsResp, areasResp, stationsResp, interchangeResp) => {
                mapApp.networkError = false;
                mapApp.neighbours = neighboursResp.data;
                mapApp.quadrants = quadResp.data;
                mapApp.bounds = boundsResp.data;
                mapApp.areas = areasResp.data;
                mapApp.stations = stationsResp.data;
                mapApp.interchanges = interchangeResp.data;
                mapApp.draw();
            })).catch(error => {
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



