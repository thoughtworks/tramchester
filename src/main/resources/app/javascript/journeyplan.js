
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
                startStop: '',
                endStop: '',
                time: getCurrentTime(),
                date: getCurrentDate(),
                journeys: [],
                notes: []
            }
        },

        methods: {
            plan(){
                axios.get('http://localhost:8080/api/journey', {
                    params: {
                        start: this.startStop, end: this.endStop, departureTime: this.time, departureDate: this.date}
                }).then(function (response) {
                    app.journeys = response.data.journeys;
                    app.notes = response.data.notes;
                    // handle success
                    console.log(response);
                })
                .catch(function (error) {
                    console.log(error);
                });
            }
        }
        ,
        mounted () {
            axios
                .get('http://localhost:8080/api/stations')
                .then(response => (
                    this.stops = response.data.stations))
                .catch(function (error) {
                    console.log(error);
                })
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
