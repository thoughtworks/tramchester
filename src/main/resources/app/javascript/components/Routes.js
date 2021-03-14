
export default { 
    classForRoute: function(route) {
        const prefix = 'RouteClass';
        if (route.transportMode=='Tram') {
            return prefix + route.shortName.replace(/\s+/g, '');
        }
        return prefix + route.transportMode;
    },

    addStations: function(map, routes) {
        routes.forEach(route => {
            this.addStationsForRoute(map, route);
        })
    },
    
    addStationsForRoute: function(map, route) {
        var stationLayerGroup = L.layerGroup();
    
        var added = []; // stations on mutliple routes

        route.stations.forEach(station => {
            if (!added.includes(station.id)) {
                var lat = station.latLong.lat;
                var lon = station.latLong.lon;
                var marker = new L.circleMarker(L.latLng(lat,lon), { title: station.name, radius: 1 })
                marker.bindTooltip(station.name + "<br> '" +station.id+ "' (" + station.transportMode+")");
                added.push(station.id);
                stationLayerGroup.addLayer(marker);
            }
        });
    
        stationLayerGroup.addTo(map);
    },

    highlightRoute: function(event) {
        var layer = event.target;

        /////////NOTE:
        // Cannot change className within event handler 
        // https://github.com/Leaflet/Leaflet/issues/2662

        layer.setStyle({
            weight: 10
        });

        layer.bringToFront();
    },

    unhighlightRoute: function(event) {
        var layer = event.target;

        layer.setStyle({
            weight: 6
        });

        layer.bringToBack();
    },
    
    addRoutes: function(map, routes) {
        var routeLayerGroup = L.layerGroup();
    
        routes.forEach(route => {
            var steps = [];
            route.stations.forEach(station => {
                if (station.latLong.valid) {
                    steps.push([station.latLong.lat, station.latLong.lon]);
                }
            })
            var line = L.polyline(steps); 
            line.bindTooltip(route.routeName + "<br>" 
                + "'"+route.id + "' (" + route.transportMode+")");
            line.setStyle({className: this.classForRoute(route), weight: 6});
            line.on({
                mouseover: this.highlightRoute,
                mouseout: this.unhighlightRoute
            });
            routeLayerGroup.addLayer(line);
        })
    
        // faster to add this way for larger numbers of lines/points
        routeLayerGroup.addTo(map);
    }, 

    findAndSetMapBounds: function(map, routes) {
        let minLat = 1000;
        let maxLat = -1000;
        let minLon = 1000;
        let maxLon = -1000;
        routes.forEach(route => {
            route.stations.forEach(position => {
                if (position.latLong.valid) {
                    var lat = position.latLong.lat;
                    if (lat < minLat) {
                        minLat = lat;
                    }
                    else if (lat > maxLat) {
                        maxLat = lat;
                    }
                    var lon = position.latLong.lon;
                    if (lon < minLon) {
                        minLon = lon;
                    }
                    else if (lon > maxLon) {
                        maxLon = lon;
                    }
                }
            });
        })
    
        var corner1 = L.latLng(minLat, minLon);
        var corner2 = L.latLng(maxLat, maxLon);
        var bounds = L.latLngBounds(corner1, corner2);
        map.fitBounds(bounds);
    }
}