
new Vue({
        el: '#journeyplan',
        data () {
            return {
                stops: null,
                startStop: null,
                endStop: null,
                time: null,
                date: null
            }
        },
        methods: {
            plan(){
                axios.get('http://localhost:8080/api/journey', {
                    params: {
                        start: this.startStop, end: this.endStop, departureTime: this.time, departureDate: this.date}
                }).then(function (response) {
                    // handle success
                    console.log(response);
                })
                .catch(function (error) {
                    // handle error
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
                    // handle error
                    console.log(error);
                })
        }
    })
