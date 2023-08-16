
export default { 
    classForRoute: function(route) {
        const prefix = 'RouteClass';
        if (route.transportMode=='Tram') {
            return prefix + route.shortName.replace(/\s+/g, '');
        }
        return prefix + route.transportMode;
    },

    addStations: function(map, routes) {
        var stationLayerGroup = L.layerGroup();

        routes.forEach(route => {
            this.addStationsForRoute(route, stationLayerGroup);
        })

        stationLayerGroup.addTo(map);

        return stationLayerGroup;
    },
    
    addStationsForRoute: function(route, stationLayerGroup) {
    
        var added = []; // stations on mutliple routes

        route.stations.forEach(station => {
            if (!added.includes(station.id)) {
                var lat = station.latLong.lat;
                var lon = station.latLong.lon;
                var marker = new L.circleMarker(L.latLng(lat,lon), { title: station.name, radius: 3 })
                marker.bindTooltip(station.name + "<br> '" +station.id+ "' (" + station.transportModes+")");
                added.push(station.id);
                stationLayerGroup.addLayer(marker);
            }
        });
    
    },

    highlightRoute: function(event) {
        var layer = event.target;

        /////////NOTE:
        // Cannot change className within event handler 
        // https://github.com/Leaflet/Leaflet/issues/2662

        layer.setStyle({
            weight: 5
        });

        layer.bringToFront();
    },

    unhighlightRoute: function(event) {
        var layer = event.target;

        layer.setStyle({
            weight: 3
        });

        layer.bringToBack();
    },
    
    addRoutes: function(map, routes) {
        var routeLayerGroup = L.layerGroup();

        let stationSeen = new Map();

        const baseOffsetPixels = 2;
    
        routes.forEach(route => {
            var steps = [];
            const inbound = route.routeID.includes(":I:");
            // since we plot the routes in specific directions (inbound vs outbound) don't need this
            // const offsetPixels = inbound ? 5 : 5;
            var mostSeen = 0;

            const direction = inbound ? "inbound" : "outbound";

            route.stations.forEach(station => {
                if (station.latLong.valid) {
                    steps.push([station.latLong.lat, station.latLong.lon]);
                }
                const stationId = station.id + "_" + direction;
                var newCount = 1;
                if (stationSeen.has(stationId)) {
                    newCount = stationSeen.get(stationId) + 1;
                }
                stationSeen.set(stationId, newCount);
                if (newCount > mostSeen) {
                    mostSeen = newCount;
                }
            })
            const offsetPixels = baseOffsetPixels + mostSeen;

            var line = L.polyline(steps, {className: this.classForRoute(route), weight: 3, opacity: 0.8}); 
            line.bindTooltip(route.routeName + "<br>"  + "'" +
                route.shortName + "' (" + 
                route.transportMode+")" + " [" + route.routeID + "]"
                );
            line.setOffset(offsetPixels);
            line.on({
                mouseover: this.highlightRoute,
                mouseout: this.unhighlightRoute
            });
            routeLayerGroup.addLayer(line);
        })
    
        // faster to add this way for larger numbers of lines/points
        routeLayerGroup.addTo(map);

        return routeLayerGroup;
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