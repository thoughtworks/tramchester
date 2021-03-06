const axios = require('axios');

export default { 
    data: function() {
        return {
            closures: []
        }
    },
    mounted() {
        var that = this
        axios.get('/api/stations/closures')
        .then(function (response) {
            that.closures = response.data;})
        .catch(function (error) {
            this.$emit('network-error')
            console.log(error)
        });
    },
   
    template: `
    <div id="closuresComponent" v-if="closures.length>0">
        <b-card bg-variant="warning" class="tramchesterApp mb-2 w-75" >
            <b-card-text>
                <ul id="ClosureList" class="Closures list-group list-group-flush">
                    <li v-for="closed in closures" id="ClosedItem">
                        {{closed.station.name}} station is closed between {{closed.begin}} and {{closed.end}}
                    </li>
                </ul>
                Visit <a href="https://www.tfgm.com/">www.tfgm.com</a> for full details
            </b-card-text>
        </b-card>
    </div>
    `
}

