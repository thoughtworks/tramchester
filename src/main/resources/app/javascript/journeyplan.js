
new Vue({
        el: '#journeyplan',
        data () {
            return {
                message: 'Hello journeyplan!',
                items: null
            }
        },
        mounted () {
            axios
                .get('http://localhost:8080/api/stations')
                .then(response => (
                    this.items = response.data.stations))
        }
    })
