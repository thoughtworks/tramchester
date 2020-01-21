
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

            var lineGenerator = d3.line(); //.curve(d3.curveCardinal);
            mapApp.svg.selectAll("line")
                .data(mapApp.positionsList).enter().append("path")
                .attr("d", function (d) {
                let normal = 10; // length of normal line, pixels

                let x1 = mapApp.scaleX(d.first);
                let y1 = mapApp.scaleY(d.first);
                let x2 = mapApp.scaleX(d.second);
                let y2 = mapApp.scaleY(d.second);
                let midX = (x1+x2) / 2;
                let midY = (y1+y2) / 2;
                let halfX = Math.abs(midX - x1);
                let halfY = Math.abs(midY - y1);

                let halfLen = Math.sqrt((halfX*halfX)+(halfY*halfY));

                let lineAngel = Math.atan(halfY/halfX);

                let endNormalDist = Math.sqrt((halfLen*halfLen)+(normal*normal));
                let endNormalAngel = Math.asin(normal/endNormalDist);

                let internalAngle = endNormalAngel+lineAngel;

                let dx = (Math.cos(internalAngle) * endNormalDist);
                let dy = (Math.sin(internalAngle) * endNormalDist);
                var normalX = 0 ; x1 + dx;
                if (x2>x1) {
                    normalX = x1 + dx;
                } else {
                    normalX = x1 - dx;
                }
                var normalY = 0; //y1 - dy;
                if (y2>y1) {
                    normalY = y1 + dy;
                } else {
                    normalY = y1 - dy;
                }

                var data = [
                    [x1, y1], [normalX, normalY], [x2, y2]];
                return lineGenerator(data);
                }).style("fill", "none").style("stroke", "black").style("stroke-width", "1px");

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