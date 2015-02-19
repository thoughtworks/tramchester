'use strict';

techLabApp.factory('mapService', function () {
    var position = null;
    return  {
        drawMap: function (lat, lon, zoomLevel) {
            var options = {
                elt: document.getElementById('map'),
                zoom: zoomLevel,
                latLng: {lat: lat, lng: lon},
                mtype: 'osm',
                bestFitMargin: 0,
                zoomOnDoubleClick: true
            };

            window.map = new MQA.TileMap(options);
        },

        drawManchesterMap: function () {
            this.drawMap(53.4800, -2.2400, 14);
        },

        dropPoint: function (lat, lon, point, text, color) {
            map.removeShape(point);
            point = new MQA.Poi({lat: lat, lng: lon});


            point.setInfoContentHTML(text);
            map.addShape(point);
            //point.toggleInfoWindow();
            map.bestFit();
            return point;
        },

        showControls: function () {
            MQA.withModule('largezoom', function () {

                map.addControl(
                    new MQA.LargeZoom(),
                    new MQA.MapCornerPlacement(MQA.MapCorner.TOP_LEFT, new MQA.Size(5, 5))
                );

            });
        },

        drawStops: function (stops) {
            for (var i = 0; i < stops.length - 2; i++) {
                var point = new MQA.Poi({lat: stops[i].lat, lng: stops[i].lon});
                point.setInfoContentHTML(stops[i].name);

                var icon = new MQA.Icon("http://jcruz661.wikispaces.com/file/view/Sphere%202.png/403924452/511x506/Sphere%202.png", 20, 20);
                point.setIcon(icon);
                map.addShape(point);
            }
        },

        drawTrams: function (trams, shapes) {
            removeRemoveTrams(shapes);
            shapes = [];

            for (var i = 0; i < trams.length; i++) {
                var point = new MQA.Poi({lat: trams[i].lat, lng: trams[i].lon});
                point.setInfoContentHTML(trams[i].routeId);

                var icon = new MQA.Icon("http://cdn1.iconfinder.com/data/icons/windows8_icons_iconpharm/26/tram.png", 20, 20);
                point.setIcon(icon);
                map.addShape(point);
                shapes.push(point);

//                MQA.withModule('shapes', function () {
//                    var line = new MQA.LineOverlay();
//                    line.setShapePoints([trams[i].lastStop.lat, trams[i].lastStop.lon, trams[i].nextStop.lat, trams[i].nextStop.lon]);
//                    map.addShape(line);
//                });
            }
            return shapes;
        },


        showDirection: function (fromPosition, toPosition) {
            MQA.withModule('directions', function () {
                map.addRoute([
                    {latLng: {lat: fromPosition.lat, lng: fromPosition.lon}},
                    {latLng: {lat: toPosition.lat, lng: toPosition.lon}}
                ],

                    {ribbonOptions: {draggable: true}, routeType: 'pedestrian'},

                    displayNarrative
                );
            });
        },

        geoLocate: function (location, getPosition) {
            MQA.withModule('nominatim', function () {
                map.nominatimSearchAndAddLocation(location, getPosition);
            });
        }

    };

    function removeRemoveTrams(shapes) {
        console.log(shapes);
        if (shapes != null) {
            for (var i = 0; i < shapes.length; i++) {
                map.removeShape(shapes[i]);
            }
        }
    }

    function displayNarrative(data) {
        if (data.route) {
            var legs = data.route.legs, html = '', i = 0, j = 0, trek, maneuver;
            html += '<table><tbody>';

            for (; i < legs.length; i++) {
                for (j = 0; j < legs[i].maneuvers.length; j++) {
                    maneuver = legs[i].maneuvers[j];
                    html += '<tr>';
                    html += '<td>';

                    if (maneuver.iconUrl) {
                        html += '<img src="' + maneuver.iconUrl + '">  ';
                    }

                    for (var k = 0; k < maneuver.signs.length; k++) {
                        var sign = maneuver.signs[k];
                        if (sign && sign.url) {
                            html += '<img src="' + sign.url + '">  ';
                        }
                    }

                    html += '</td><td>' + maneuver.narrative + '</td>';
                    html += '<td>'
//                    if (maneuver.mapUrl) {
//                        html += '<img src="' + maneuver.mapUrl + '" style="height:40px;">';
//                    } else {
//                        html += '&nbsp;'
//                    }
                    html += '</td>'
                    html += '</tr>';
                }
            }

            html += '</tbody></table>';
            document.getElementById('narrative').innerHTML = html;
        }
    }
});