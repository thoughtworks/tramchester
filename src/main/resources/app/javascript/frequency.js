
const axios = require('axios');

var Vue = require('vue');
Vue.use(require('bootstrap-vue'));
var oboe = require('oboe');

var L = require('leaflet');

require('file-loader?name=[name].[ext]!../frequency.html');

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

function addBoxWithFrequency(boxWithFrequency) {
    const bounds = [[boxWithFrequency.bottomLeft.lat, boxWithFrequency.bottomLeft.lon], 
        [boxWithFrequency.topRight.lat, boxWithFrequency.topRight.lon]];

    const numberOfStopCalls = boxWithFrequency.numberOfStopcalls;

    var colour = getColourForFrequency(boxWithFrequency);     
    var rectangle = L.rectangle(bounds, {weight: 0, color: colour, fillColor: colour, fill: true, fillOpacity: 0.5});
    if (numberOfStopCalls>0) {
        rectangle.bindTooltip('numer of buses ' + numberOfStopCalls);
        //rectangle.on('click', boxClicked, boxWithFrequency);
    }
    rectangle.addTo(mapApp.map);
}

function getColourForFrequency(boxWithFrequency) {
    const numberOfStopCalls = boxWithFrequency.numberOfStopcalls;
    if (numberOfStopCalls==0) {
        return "#ff0000";
    }

    var limit = 10;
    var greenString = "00";
    if (numberOfStopCalls > 0) {
        var value = Math.floor((255 / limit) * numberOfStopCalls);
        if (value>254) {
            value = 254;
        }
        greenString = value.toString(16);
        if (greenString.length == 1) {
            greenString = '0' + greenString;
        }
    }
    return '#00'+greenString+'00';
}

function queryForFrequencies(gridSize, date, startTime, endTime) {
    var urlParams = { gridSize: gridSize, date: date, startTime: startTime, endTime: endTime};

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
                // make sure to use HH:MM format with leading zero
                // YYYY-MM-DD
                //var date = getCurrentDate();
                var date = "2021-10-25"
                queryForFrequencies(500, date, "07:30", "08:30");
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
    oboe('/api/frequency?' + searchParams.toString())
        .node('BoxWithFrequency', function (box) {
            addBoxWithFrequency(box);
        })
        .fail(function (errorReport) {
            console.log("Failed to load grid '" + errorReport.toString() + "'");
        });
}

