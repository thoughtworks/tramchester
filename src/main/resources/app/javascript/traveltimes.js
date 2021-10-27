
const axios = require('axios');

var Vue = require('vue');
Vue.use(require('bootstrap-vue'));
var oboe = require('oboe');

var L = require('leaflet');

require('file-loader?name=[name].[ext]!../traveltimes.html');

import 'bootstrap/dist/css/bootstrap.css';
import 'bootstrap-vue/dist/bootstrap-vue.css';
import 'leaflet/dist/leaflet.css'
import './../css/tramchester.css'

L.Icon.Default.imagePath = '/app/dist/images/';
require("leaflet/dist/images/marker-icon-2x.png");
require("leaflet/dist/images/marker-shadow.png");

import Footer from './components/Footer';
import Routes from './components/Routes';

function getCurrentDate() {
    const now = new Date().toISOString();
    return now.substr(0,  now.indexOf("T")); // iso-8601 date part only as YYYY-MM-DD
}

function boxClicked(event) {
    mapApp.journeyLayer.clearLayers();
    var journey = this.journey.journey;
    var steps = [];

    journey.path.forEach(item => {
        steps.push([item.latLong.lat, item.latLong.lon]);
    })
    var line = L.polyline( steps, { color: 'red' });
    mapApp.journeyLayer.addLayer(line);
    mapApp.journeyLayer.addTo(mapApp.map);
}

function addBoxWithCost(boxWithCost) {
    const bounds = [[boxWithCost.bottomLeft.lat, boxWithCost.bottomLeft.lon], 
        [boxWithCost.topRight.lat, boxWithCost.topRight.lon]];

    var colour = getColourForCost(boxWithCost);     
    var rectangle = L.rectangle(bounds, {weight: 1, color: colour, fillColor: colour, fill: true, fillOpacity: 0.5});
    if (boxWithCost.minutes>0) {
        rectangle.bindTooltip('cost ' + boxWithCost.minutes);
        rectangle.on('click', boxClicked, boxWithCost);
    }
    rectangle.addTo(mapApp.map);
}

function getColourForCost(boxWithCost) {
    if (boxWithCost.minutes==0) {
        return "#0000ff";
    }
    if (boxWithCost.minutes < 0) {
        return "#ff0000";
    }
    var greenString = "00";
    if (boxWithCost.minutes > 0) {
        var red = Math.floor((255 / 112) * (112 - boxWithCost.minutes));
        greenString = red.toString(16);
        if (greenString.length == 1) {
            greenString = '0' + greenString;
        }
    }
    return '#00'+greenString+'00';
}

function queryForGridLatLong(gridSize, lat, lon, departureTime, departureDate, maxChanges, maxDuration) {
    var urlParams = {
        destination: "MyLocationPlaceholderId", gridSize: gridSize, departureTime: departureTime, departureDate: departureDate, 
        lat: lat, lon: lon,
        maxChanges: maxChanges, maxDuration: maxDuration};

    const searchParams = new URLSearchParams(urlParams);

    getGrids(searchParams);
}

var mapApp = new Vue({
    el: '#tramMap',
    components: {
        'app-footer' : Footer
    },
    data() {
        return {
            map: null,
            grid: null,
            journeyLayer: null,
            networkError: false,
            routes: [],
            feedinfo: [],
        }
    },
    methods: {
        networkErrorOccured() {
            app.networkError = true;
        },
        draw() {
            Routes.findAndSetMapBounds(mapApp.map, mapApp.routes);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(mapApp.map);
            //Routes.addRoutes(mapApp.map, mapApp.routes);
            //addStations();
            mapApp.journeyLayer = L.featureGroup()
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
        axios.get("/api/routes")
            .then(function (response) {
                mapApp.networkError = false;
                mapApp.routes = response.data;
                mapApp.draw();
                // 9400ZZMASTP 9400ZZMAAIR 1800MABS001 MAN
                // make sure to use HH:MM format with leading zero
                //queryForGrid(1000, "POSTCODE_M23AA", "08:15", getCurrentDate(), "3", "360");
                // man picc 53.4774286,-2.2313236
                queryForGridLatLong(1000, "53.4774286", "-2.2313236", "07:30", getCurrentDate(), "2", "60");
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


function getGrids(searchParams) {
    oboe('/api/grid?' + searchParams.toString())
        .node('BoxWithCost', function (box) {
            addBoxWithCost(box);
        })
        .fail(function (errorReport) {
            console.log("Failed to load grid '" + errorReport.toString() + "'");
        });
}

