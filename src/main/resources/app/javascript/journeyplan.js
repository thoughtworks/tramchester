
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
                noResults: false,
                journeyFields: [
                    {key:'firstDepartureTime',label:'Departs',sortable:true, tdClass:'departTime'},
                    {key:'expectedArrivalTime',label:'Arrives',sortable:true, tdClass:'arriveTime'},
                    {key:'summary', label:'Changes', tdClass:'changes'},
                    {key:'heading',label:'Summary',tdClass:'summary'} ],
                stageFields: [{key:'firstDepartureTime',label:'Time',tdClass:'departTime'},
                    {key:'prompt',label:'Action',tdClass:'action' },
                    {key:'actionStation.name',label:'Station', tdClass:'actionStation'},
                    {key:'platform.platformNumber', label:'Platform', tdClass:'platform'},
                    {key:'headSign', label:'Towards', tdClass:'headsign'},
                    {key:'summary', label:'Line', tdClass: this.stageRowClass },
                    {key:'passedStops', label:'Stops', tdClass:'passedStops'}]
            }
        },
        methods: {
            clearJourneysAndNotes() {
                while(app.journeys.length>0) {
                    app.journeys.pop();
                }
                 while(app.notes.length>0) {
                    app.notes.pop();
                }
            },
            plan(event){
                app.clearJourneysAndNotes();
                event.preventDefault(); // stop page reload
                axios.get('/api/journey', {
                    params: {
                        start: this.startStop, end: this.endStop, departureTime: this.time, departureDate: this.date}
                }).then(function (response) {
                    app.journeys = app.journeys.concat(response.data.journeys);
                    app.noResults = app.journeys.length==0;
                    app.notes = app.notes.concat(response.data.notes);
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
            stageRowClass(value, header, item) {
                if (value && header === 'summary') {
                    return item.displayClass;
                }
                return null;
            },
            setCookie() {
                var cookie = { 'visited' : true };
                var expiry = moment().add(100, 'days').toDate();
                this.$cookies.set("tramchesterVisited",cookie,"1d");
            },
            timeToNow() {
                app.time = getCurrentTime();
            },
            dateToNow() {
                app.date = getCurrentDate();
            }
        }
        ,
        mounted () {
            var cookie = this.$cookies.get("tramchesterVisited");
            if (cookie==null) {
                this.$refs.cookieModal.show();
            }
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
