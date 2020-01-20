

const axios = require('axios');
var _ = require('lodash');
var Vue = require('vue');
Vue.use(require('vue-cookies'));
Vue.use(require('bootstrap-vue'));

var d3 = require("d3");

import 'bootstrap/dist/css/bootstrap.css';
import 'bootstrap-vue/dist/bootstrap-vue.css';
import './../css/tramchester.css'

var width = 800;
var height = 800;

function initD3(app) {
    app.svg = d3.select("#map").append("svg")
        .attr("width", width)
        .attr("height", height);
}

const mapApp = new Vue({
    el: '#tramMap',
    data() {
        return {
            positions: null,
            networkError: false,
            projection: null,
            path: null,
            svg: null
        }
    },
    methods: {
        draw() {
            let minLat = 1000;
            let maxLat = -1000;
            let minLon = 1000;
            let maxLon = -1000;
            mapApp.positions.positionsList.forEach(position => {
                var lat = position.first.latLong.lat;
                if (lat<minLat) {
                    minLat = lat;
                } else if (lat>maxLat) {
                    maxLat = lat;
                }
                var lon = position.first.latLong.lon;
                if (lon<minLon) {
                    minLon = lon;
                } else if (lon>maxLon) {
                    maxLon = lon;
                }
            });
            let lonOffset = 0-minLon;
            let latOffset = 0-minLat;
            let scaleLon = width / (maxLon-minLon);
            let scaleLat = height / (maxLat-minLat);
            mapApp.svg.selectAll("circle")
                .data(mapApp.positions.positionsList)
                .enter().append("circle").attr("fill", "red")
                .attr("cx", d => (d.first.latLong.lon + lonOffset) * scaleLon)
                .attr("cy", d => height-((d.first.latLong.lat + latOffset) * scaleLat))
                .attr("r", 5).attr("title", d => d.first.name);
            mapApp.svg.selectAll("circle")
                .data(mapApp.positions.positionsList)
                .enter().append("circle").attr("fill", "red")
                .attr("cx", d => (d.second.latLong.lon + lonOffset) * scaleLon)
                .attr("cy", d => height-((d.second.latLong.lat + latOffset) * scaleLat))
                .attr("r", 5).attr("title", d => d.second.name);
        }
    },
    mounted () {
        axios
            .get('/api/positions')
            .then(function (response) {
                mapApp.networkError = false;
                mapApp.positions = response.data;
                mapApp.draw(mapApp.positions);
            })
            .catch(function (error) {
                mapApp.networkError = true;
                console.log(error);
            });
        initD3(this);
    }
});