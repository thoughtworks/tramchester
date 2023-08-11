
const axios = require('axios');

var Vue = require('vue');
var oboe = require('oboe');

var L = require('leaflet');

require('file-loader?name=[name].[ext]!../frequency.html');

import VueSlider from 'vue-slider-component'


import 'bootstrap/dist/css/bootstrap.css';
import 'leaflet/dist/leaflet.css'
import './../css/tramchester.css'
import 'vue-slider-component/theme/default.css'

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
    const modes = boxWithFrequency.modes;

    const frequency = getFrequency(numberOfStopCalls);
    const colour = getColourForFrequency(frequency);     
    const rectangle = L.rectangle(bounds, {weight: 0, color: colour, fillColor: colour, fill: true, fillOpacity: 0.5});
    if (numberOfStopCalls>0) {
        const displayFrequency = Math.round(frequency*100) / 100; 
        rectangle.bindTooltip(modes +' per hour ' + displayFrequency);
    } else {
        rectangle.bindTooltip('no services');
    }
    rectangle.addTo(mapApp.frequencyLayer);
}

function getFrequency(totalNumber) {
    if (totalNumber==0) {
        return 0;
    }
    const periodHours = mapApp.hours[1] - mapApp.hours[0];
    return totalNumber / periodHours;
}

function getColourForFrequency(frequency) {
    if (frequency==0) {
        return "#ff0000";
    }

    var limit = 4;
    var greenString = "01";
    if (frequency > 0) {
        var value = Math.floor((255 / limit) * frequency);
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

    mapApp.frequencyLayer.clearLayers();
    getFrequencies(searchParams);
    mapApp.frequencyLayer.addTo(mapApp.map);
}

var mapApp = new Vue({
    el: '#frequencymap',
    components: {
        'app-footer': Footer,
        'VueSlider': VueSlider
    },
    data() {
        return {
            map: null,
            grid: null,
            hours: [ 10, 11],
            frequencyLayer: null,
            networkError: false,
            routes: [],
            feedinfo: [],
            date: getCurrentDate()
        }
    },
    methods: {
        networkErrorOccured() {
            app.networkError = true;
        },
        setupMap() {
            Routes.findAndSetMapBounds(mapApp.map, mapApp.routes);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(mapApp.map);
            //Routes.addRoutes(mapApp.map, mapApp.routes);
            //addStations();
            mapApp.frequencyLayer = L.featureGroup();
        },
        draw() {
            const startTimeTxt = mapApp.hours[0].toString();
            const endTimeTxt = mapApp.hours[1].toString();
            const startTime = startTimeTxt.padStart(2,'0') + ":00";
            const endTime = endTimeTxt.padStart(2,'0') + ":00";
            queryForFrequencies(500, mapApp.date, startTime, endTime);
        },
        dateToNow() {
            mapApp.date = getCurrentDate();
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
                mapApp.setupMap();
                // make sure to use HH:MM format with leading zero
                // YYYY-MM-DD
                //var date = getCurrentDate();
                //var date = "2021-10-25"
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


function getFrequencies(searchParams) {
    oboe('/api/frequency?' + searchParams.toString())
        .node('BoxWithFrequency', function (box) {
            addBoxWithFrequency(box);
        })
        .fail(function (errorReport) {
            console.log("Failed to load grid '" + errorReport.toString() + "'");
        });
}

