
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
var margin = 24;

function initD3(app) {
    app.svg = d3.select("#map").append("svg")
        .attr("width", width)
        .attr("height", height);
}

var mapApp = new Vue({
    el: '#tramMap',
    data() {
        return {
            positionsList: null,
            uniqueStations: [],
            networkError: false,
            projection: null,
            path: null,
            svg: null,
            lonOffset: 0,
            latOffset: 0,
            scaleLon: 0,
            scaleLat: 0
        }
    },
    methods: {
        filterPositions() {
            var ids = [];
            mapApp.positionsList.forEach(item => {
                if (ids.indexOf(item.first.id)<0) {
                    ids.push(item.first.id);
                    mapApp.uniqueStations.push(item.first);
                }
                if (ids.indexOf(item.second.id)<0) {
                    ids.push(item.second.id);
                    mapApp.uniqueStations.push(item.second);
                }
            });
            let minLat = 1000;
            let maxLat = -1000;
            let minLon = 1000;
            let maxLon = -1000;
            mapApp.uniqueStations.forEach(position => {
                var lat = position.latLong.lat;
                if (lat<minLat) {
                    minLat = lat;
                } else if (lat>maxLat) {
                    maxLat = lat;
                }
                var lon = position.latLong.lon;
                if (lon<minLon) {
                    minLon = lon;
                } else if (lon>maxLon) {
                    maxLon = lon;
                }
            });
            mapApp.lonOffset = 0-minLon;
            mapApp.latOffset = 0-minLat;
            mapApp.scaleLon = (width-margin) / (maxLon-minLon);
            mapApp.scaleLat = (height-margin) / (maxLat-minLat);
        },
        draw() {
            mapApp.svg.selectAll("circle")
                .data(mapApp.uniqueStations)
                .enter().append("circle").attr("fill", "red")
                .attr("cx", mapApp.scaleX)
                .attr("cy", mapApp.scaleY)
                .attr("r", 3).attr("title", d => d.name).attr("id", d=> d.id);

            var lineGenerator = d3.line().curve(d3.curveCardinal);
            mapApp.svg.selectAll("line")
                .data(mapApp.positionsList).enter().append("path")
                .attr("d", function (d) {
                    var data = [
                        [mapApp.scaleX(d.first), mapApp.scaleY(d.first)],
                        [mapApp.scaleX(d.second),mapApp.scaleY(d.second)]];
                    return lineGenerator(data);
                })
                .style("fill", "none")
                .style("stroke", "black")
                .style("stroke-width", "1px");

        },
        scaleY(station) {
            return ((height-(margin/2)) - ((station.latLong.lat + mapApp.latOffset) * mapApp.scaleLat)); //+ (margin / 2);
        },
        scaleX(station) {
            return ((station.latLong.lon + mapApp.lonOffset) * mapApp.scaleLon) + (margin / 2);
        }
    },
    mounted () {
        axios
            .get('/api/positions')
            .then(function (response) {
                mapApp.networkError = false;
                mapApp.positionsList = response.data.positionsList;
                mapApp.filterPositions();
                mapApp.draw();
            })
            .catch(function (error) {
                mapApp.networkError = true;
                console.log(error);
            });
        initD3(this);
    }
});