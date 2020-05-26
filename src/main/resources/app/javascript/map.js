
const axios = require('axios');
var _ = require('lodash');
var Vue = require('vue');
Vue.use(require('vue-cookies'));
Vue.use(require('bootstrap-vue'));

var d3 = require("d3");
var L = require('leaflet');

import 'bootstrap/dist/css/bootstrap.css';
import 'bootstrap-vue/dist/bootstrap-vue.css';
import 'leaflet/dist/leaflet.css'
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

// find angle of line between first and second
function textRotation(first, second) {
    let x1 = mapApp.scaleX(first);
    let y1 = mapApp.scaleY(first);
    let x2 = mapApp.scaleX(second);
    let y2 = mapApp.scaleY(second);

    let midX = (x1+x2) / 2;
    let midY = (y1+y2) / 2;
    let halfX = midX - x1;
    let halfY = midY - y1;

    let lineAngel = Math.atan(halfY/halfX);
    return (lineAngel  * (180 / Math.PI));
}

// some trig to find (x,y) of end of a normal line that started mid-point between first and second
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
            map: null,
            positionsList: null,
            buses: false,
            uniqueStations: [],
            busLinks: [],
            tramLinks: [],
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
                if (item.first.tram && item.second.tram) {
                    mapApp.tramLinks.push(item);
                } else {
                    mapApp.busLinks.push(item);
                }
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
            mapApp.map.setView([51.505, -0.09], 13);

            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(mapApp.map);

            L.marker([51.5, -0.09]).addTo( mapApp.map)
                .bindPopup('A pretty CSS3 popup.<br> Easily customizable.')
                .openPopup();

            // lines between tram stations
            var lineGenerator = d3.line().curve(d3.curveCardinal);
            mapApp.svg.selectAll("line")
                    .data(mapApp.tramLinks).enter().append("path")
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
                }).style("fill", "none").style("stroke", "darkgrey").style("stroke-width", "2px");

                 // lines between bus stations
                var lineGenerator = d3.line();
                mapApp.svg.selectAll("line")
                        .data(mapApp.busLinks).enter().append("path")
                        .attr("id", d=> d.first.id+d.second.id)
                        .attr("d", function (d) {

                        let x1 = mapApp.scaleX(d.first);
                        let y1 = mapApp.scaleY(d.first);
                        let x2 = mapApp.scaleX(d.second);
                        let y2 = mapApp.scaleY(d.second);

                        var points = [[x1, y1], [x2, y2]];
                        return lineGenerator(points);
                    }).style("fill", "none").style("stroke", "grey").style("stroke-width", "1px");

            // tram stations
            mapApp.svg.selectAll("circle")
                .data(mapApp.uniqueStations.filter(d=> d.tram))
                .enter().append("circle").attr("fill", "blue")
                .attr("cx", mapApp.scaleX)
                .attr("cy", mapApp.scaleY)
                .attr("r", 2).attr("title", d => d.name).attr("id", d=> d.id);

            // bus stations
            mapApp.svg.selectAll("circle")
                .data(mapApp.uniqueStations.filter(d=> !d.tram))
                .enter().append("circle").attr("fill", "green")
                .attr("cx", mapApp.scaleX)
                .attr("cy", mapApp.scaleY)
                .attr("r", 1).attr("title", d => d.name).attr("id", d=> d.id);

            // station labels on map, only when trams only, too cluttered otherwise
            if (!mapApp.buses) {
                // only have one pair for each location
                var ids = [];
                var oneDirectionOnly = [];
                mapApp.positionsList.forEach(item => {
                    if (ids.indexOf(item.first.id)<0) {
                        ids.push(item.first.id);
                        oneDirectionOnly.push(item);
                    }
                });

                mapApp.svg.selectAll("g")
                    .data(oneDirectionOnly).enter().append("g")
                    .attr("transform", function(d) {
                        let x = mapApp.scaleX(d.first);
                        let y = mapApp.scaleY(d.first);
                        let angle = textRotation(d.first, d.second)+90;
                        if (angle>90) {
                            angle = 180-angle;
                        }
                        return "translate(" + x +","+ y + ") rotate("+ angle +")";
                    })
                    .append("text")
                    .text(d => d.first.name)
                    .attr("font-family", "sans-serif").attr("font-size", "8px").attr("fill", "blue");
            }

            // live tram labels
            mapApp.svg.selectAll("g")
                .data(mapApp.tramLinks).enter().append("g")
                .attr("id", d=> "between"+d.first.id+d.second.id)
                .attr("transform", function(d) {
                    let normalPoint = normal(d.first,d.second,15);
                    return "translate( " + normalPoint[0] +", "+ normalPoint[1] + ")"
                    + " rotate(" + textRotation(d.first,d.second) + ")";
                })
                .append("text")
                .text(d => textFor(d.trams))
                .attr("font-family", "sans-serif").attr("font-size", "8px").attr("fill", "red");
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
            .get('/api/positions?unfiltered=true')
            .then(function (response) {
                mapApp.networkError = false;
                mapApp.positionsList = response.data.positionsList;
                mapApp.buses = response.data.buses;
                mapApp.filterPositions();
                mapApp.draw();
            })
            .catch(function (error) {
                mapApp.networkError = true;
                console.log(error);
            });
        initD3(this);

        L.Icon.Default.imagePath = 'leaflet/dist/images/';
        this.map = L.map('leafletMap');

    }
});