
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
        }
    })
