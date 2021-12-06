
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
    var marker = new L.circleMarker(L.latLng(lat, lon), { title: station.name, radius: 2 });
    marker.bindTooltip(station.name + "<br> '" + station.id + "' (" + station.transportModes + ")");
    displayed.push(station.id);
    stationLayerGroup.addLayer(marker);
}

function getColourFor(mode) {
    switch(mode) {
        case "Tram": return "Blue";
        case "Bus": return "Green";
        case "Train": return "Red";
        case "Walk": return "Purple"
        default: return "Grey";
    }
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
            neighbours: [],
            comps: [],
            quadrants: [],
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
            var linkLayerGroup = L.layerGroup();
        
            links.forEach(link => {
                var steps = [];
                steps.push([link.begin.latLong.lat, link.begin.latLong.lon]);
                steps.push([link.end.latLong.lat, link.end.latLong.lon]);
                link.transportModes.forEach(mode => {
                    var line = L.polyline(steps); // hurts performance .arrowheads({ size: '5px', frequency: 'endonly' }); 
                    line.bindTooltip(mode + " between " + link.begin.name + " and " + link.end.name); 
                    var colour = getColourFor(mode);
                    line.setStyle({color: colour, opacity: 0.6});
                    linkLayerGroup.addLayer(line);
                })
            })
        
            // faster to add this way for larger numbers of lines/points
            linkLayerGroup.addTo(map);
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
                var parent = group.parent;
                shape.bindTooltip(parent.name + "<br>  (" + parent.transportModes + ")");
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

            mapApp.addLinks(mapApp.map, mapApp.links, "blue");
            mapApp.addLinks(mapApp.map, mapApp.neighbours, "green");
            mapApp.addStations(mapApp.map, mapApp.links);
            mapApp.addGroups(mapApp.map, mapApp.groups);
            mapApp.addQuadrants(mapApp.map, mapApp.quadrants);
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
            axios.get("/api/links/all"),
            axios.get("/api/links/neighbours"),
            axios.get("/api/links/composites"),
            axios.get("/api/links/quadrants")
        ]).then(axios.spread((linksResp, neighboursResp, compsResp, quadResp) => {
                mapApp.networkError = false;
                mapApp.links = linksResp.data;
                mapApp.neighbours = neighboursResp.data;
                mapApp.groups = compsResp.data;
                mapApp.quadrants = quadResp.data;
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



