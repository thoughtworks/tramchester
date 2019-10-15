
function getCurrentTime() {
    return moment().format("HH:mm");
}

function getCurrentDate() {
    return moment().format("YYYY-MM-DD")
}

app = new Vue({
        el: '#journeyplan',
        data () {
            return {
                stops: [],
                stopToProxGroup: new Map(),
                startStop: null,
                endStop: null,
                time: getCurrentTime(),
                date: getCurrentDate(),
                journeys: [],
                notes: [],
                buildNumber: '',
                feedinfo: [],
                journeyFields: [
                    {key:'firstDepartureTime',label:'Departs',sortable:true},
                    {key:'expectedArrivalTime',label:'Arrives',sortable:true},
                    {key:'summary', label:'Changes'},{key:'heading',label:'Summary'} ],
                stageFields: [{key:'firstDepartureTime',label:'Time'},
                    {key:'prompt',label:'Action' },
                    {key:'actionStation.name',label:'Station'},
                    {key:'platform.platformNumber', label:'Platform'},
                    {key:'headSign', label:'Towards'},
                    {key:'summary', label:'Line'},
                    {key:'passedStops', label:'Stops'}]
            }
        },
        methods: {
            plan(event){
                event.preventDefault(); // stop page reload
                axios.get('/api/journey', {
                    params: {
                        start: this.startStop, end: this.endStop, departureTime: this.time, departureDate: this.date}
                }).then(function (response) {
                    app.journeys = response.data.journeys;
                    app.notes = response.data.notes;
                    app.getStations(); // recent stations will have changed
                })
                .catch(function (error) {
                    console.log(error);
                });
            },
            getStations() {
                axios
                    .get('/api/stations')
                    .then(function (response) {
                        // respect way vue bindings work, can't just assign/overwrite existing list
                        changes = response.data.stations.filter(station =>
                            station.proximityGroup.order != app.stopToProxGroup.get(station.id) );
                        changes.forEach(function(change) {
                            app.stopToProxGroup.set(change.id, change.proximityGroup.order);
                        });

                        app.stops = app.stops.filter(stop =>
                            stop.proximityGroup.order === app.stopToProxGroup.get(stop.id) ); // keep unchanged

                        changes.forEach(function(change) {
                            app.stops.push(change);
                        });
                    })
                    .catch(function (error) {
                        console.log(error);
                    });
            },
            expandStages(row,index) {
                row._showDetails = !row._showDetails;
            },
            stageRowClass(item,type) {
                if (item && type === 'row') {
                    return item.displayClass;
                }
                return null;
            }
        }
        ,
        mounted () {
            this.getStations();
            axios
                .get('/api/feedinfo')
                .then(response => (
                    this.feedinfo = response.data))
                .catch(function (error) {
                    console.log(error);
                });
            axios
                .get('/api/version')
                .then(response => (
                    this.buildNumber = response.data.buildNumber))
                .catch(function (error) {
                    console.log(error);
                });
        },
        computed: {
            // startStops: function () {
            //     return app.stops;
            // },
            proxGroups: function () {
                proxGroups = [];
                seen = [];
                this.stops.forEach(stop =>
                    {
                        if (!seen.includes(stop.proximityGroup.order)) {
                            proxGroups.push(stop.proximityGroup);
                            seen.push(stop.proximityGroup.order);
                        }
                    } );
                return _.sortBy(proxGroups, [function(grp) { return grp.order; }]);
            },
            endStops: function () {
                return this.stops.filter(item => item.id!=this.startStop);
            }
        }

    })
