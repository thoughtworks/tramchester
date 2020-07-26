
export default { 
    addStations: function(map, routes) {
        routes.forEach(route => {
            if (route.transportMode!='Bus') 
            {
                this.addStationsForRoute(map, route);
            }
        })
    },
    
    addStationsForRoute: function(map, route) {
        var stationLayerGroup = L.featureGroup();
    
        route.stations.forEach(station => {
            var lat = station.latLong.lat;
            var lon = station.latLong.lon;
            var marker = new L.circleMarker(L.latLng(lat,lon), { title: station.name, radius: 1 })
            marker.bindTooltip(station.name + " (" + station.transportMode+")");
            stationLayerGroup.addLayer(marker);
        });
    
        stationLayerGroup.addTo(map);
    },
    
    addRoutes: function(map, routes) {
        var routeLayerGroup = L.featureGroup();
    
        routes.forEach(route => {
            var steps = [];
            route.stations.forEach(station => {
                steps.push([station.latLong.lat, station.latLong.lon]);
            })
            var line = L.polyline(steps, {className: route.displayClass});
            line.bindTooltip(route.routeName + " (" + route.transportMode+")");
            routeLayerGroup.addLayer(line);
        })
    
        routeLayerGroup.addTo(map);
    }, 

    findAndSetMapBounds: function(map, routes) {
        let minLat = 1000;
        let maxLat = -1000;
        let minLon = 1000;
        let maxLon = -1000;
        routes.forEach(route => {
            route.stations.forEach(position => {
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
            });
        })
    
        var corner1 = L.latLng(minLat, minLon);
        var corner2 = L.latLng(maxLat, maxLon);
        var bounds = L.latLngBounds(corner1, corner2);
        map.fitBounds(bounds);
    }
}