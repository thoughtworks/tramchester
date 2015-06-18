'use strict';

techLabApp.factory('mapService', function () {
    var position = null;
    return {
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