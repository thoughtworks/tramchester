
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
                startStop: null,
                endStop: null,
                time: getCurrentTime(),
                date: getCurrentDate(),
                journeys: [],
                journeyFields: [
                    {key:'firstDepartureTime',label:'Departs',sortable:true},
                    {key:'expectedArrivalTime',label:'Arrives',sortable:true},
                    {key:'summary', label:'Changes'},{key:'heading',label:'Summary'} ],
                notes: [],
                buildNumber: '',
                currentJourneyStages: [],
                feedinfo: [],
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
            plan(){
                axios.get('/api/journey', {
                    params: {
                        start: this.startStop, end: this.endStop, departureTime: this.time, departureDate: this.date}
                }).then(function (response) {
                    app.journeys = response.data.journeys;
                    app.notes = response.data.notes;
                })
                .catch(function (error) {
                    console.log(error);
                });
            },
            expandStages(row,index) {
                app.currentJourneyStages = row.stages;
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
            axios
                .get('/api/stations')
                .then(response => (
                    this.stops = response.data.stations))
                .catch(function (error) {
                    console.log(error);
                });
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
            startStops: function () {
                return this.stops;
            },
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
