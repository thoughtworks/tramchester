
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
var margin = 60;

function initD3(app) {
    app.svg = d3.select("#map").append("svg")
        .attr("width", width)
        .attr("height", height)
        .call(d3.zoom().on("zoom", function () {
                app.svg.attr("transform", d3.event.transform);
                var scale = 1/d3.event.transform.k;
                app.svg.selectAll("text").attr("transform","scale(" + scale + ")");
            }));
}

function textFor(trams) {
    var text = "";
    for (var i = 0; i < trams.length; i++) {
        if (i>0) text = text + "\n";
        text = text + trams[i].destination;
    }
    return text;
}

function normal(first, second, normal) {
    let x1 = mapApp.scaleX(first);
    let y1 = mapApp.scaleY(first);
    let x2 = mapApp.scaleX(second);
    let y2 = mapApp.scaleY(second);

    let midX = (x1+x2) / 2;
    let midY = (y1+y2) / 2;
    let halfX = Math.abs(midX - x1);
    let halfY = Math.abs(midY - y1);

    let lineAngel = Math.atan(halfY/halfX); // angle of line between the two points
    let halfLen = Math.sqrt((halfX*halfX)+(halfY*halfY)); // half way between the two points
    let endNormalDist = Math.sqrt((halfLen*halfLen)+(normal*normal)); // distance from (x1,y1) to end of normal line
    let endNormalAngel = Math.asin(normal/endNormalDist); // angle to tip of normal line

    let internalAngle = endNormalAngel+lineAngel;
    let dx = (Math.cos(internalAngle) * endNormalDist); // distance X to end normal line
    let dy = (Math.sin(internalAngle) * endNormalDist); // distance Y to end normal line
    var normalX = 0 ;
    if (x2>x1) {
        normalX = x1 + dx;
    } else {
        normalX = x1 - dx;
    }
    var normalY = 0;
    if (y2>y1) {
        normalY = y1 + dy;
    } else {
        normalY = y1 - dy;
    }
    return [normalX, normalY];
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
            // work out bounding box size
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
            var lines = mapApp.svg.selectAll("line")
                    .data(mapApp.positionsList).enter().append("path")
                    .attr("id", d=> d.first.id+d.second.id)
                    .attr("d", function (d) {
                    let normalLen = 3; // length of normal line, pixels

                    let x1 = mapApp.scaleX(d.first);
                    let y1 = mapApp.scaleY(d.first);
                    let x2 = mapApp.scaleX(d.second);
                    let y2 = mapApp.scaleY(d.second);

                    let normalPoint = normal(d.first, d.second, normalLen);
                    var points = [[x1, y1], normalPoint, [x2, y2]];
                    return lineGenerator(points);
                }).style("fill", "none").style("stroke", "black").style("stroke-width", "1px");

             mapApp.svg.selectAll("g")
                        .data(mapApp.positionsList).enter().append("g")
                        .attr("transform", function(d) {
                            let normalPoint = normal(d.first,d.second,10);
                            return "translate( " + normalPoint[0] +","+ normalPoint[1] + ")";
                        }).append("text").text(d => textFor(d.trams)).attr("font-family", "sans-serif").attr("font-size", "7px");


//            var text =  mapApp.svg.selectAll("text")
//                             .data(mapApp.positionsList).enter().append("text")
//                             .attr("x", d=>normal(d.first,d.second,10)[0])
//                             .attr("y", d=>normal(d.first,d.second,10)[1])
//                             .text(d => textFor(d.trams)).attr("font-family", "sans-serif").attr("font-size", "7px");
        },
        scaleY(station) {
            return ((height-(margin/2)) - ((station.latLong.lat + mapApp.latOffset) * mapApp.scaleLat));
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