

function dueTimeFormatter(value, key, row) {
    return value.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
}

export default { 
    props: ['livedataresponse'],
    data: function () {
        return {
            itemsPerPage: 5,
            departureFields: [
                {key:'transportMode', label:'type', tdClass:'transportMode', sortable: true},
                {key:'from', label:'From', tdClass:'departureDueFrom', sortable:true},
                {key:'dueTimeAsDate', label:'Time', tdClass:'departureDueTime', formatter: dueTimeFormatter, sortable:true},
                {key:'carriages', label:'', tdClass:'departureCarriages'},
                {key:'status', label:'Status', tdClass:'departureStatus'},
                {key:'destination', label:'Towards', tdClass:'departureTowards',  sortable:true}
            ],
            headers: [
                {value:'transportMode', text:'type', cellClass:'transportMode', sortable: true},
                {value:'from', text:'From', cellClass:'departureDueFrom', sortable:true},
                {value:'dueTimeAsDate', text:'Time', cellClass:'departureDueTime', sortable: true}, //formatter: dueTimeFormatter, sortable:true},
                {value:'carriages', text:'', cellClass:'departureCarriages'},
                {value:'status', text:'Status', cellClass:'departureStatus'},
                {value:'destination', text:'Towards', cellClass:'departureTowards',  sortable:true}
            ],
            sortBy: [{ key: 'dueTimeAsDate', order: 'asc' }]
        }
    },
    computed: { 
        localDueTrams: function() {
            if (this.livedataresponse==null) {
                return [];
            }
            return this.livedataresponse.departures;
        },
        noLiveResults: function() {
            if (this.livedataresponse==null) {
                return false;
            }
            return this.livedataresponse.departures.length==0;
        },
        // pageCount () {
        //     return Math.ceil(this.localDueTrams.length / this.itemsPerPage)
        //   },
    },
    template: `
    <div class="container" id="departuesView">

       
        <div id="departuresTable" v-if="localDueTrams.length>0">
            <v-data-table id="departures"
                :headers="headers" :items="localDueTrams"
                v-model:sort-by="sortBy"
                :items-per-page="itemsPerPage"
                class="elevation-1">
            </v-data-table>
        </div>
            
        <div id="noLiveResults" class="col pl-0" v-if="noLiveResults">
            <div class="card bg-light border-dark">
                <div class="card-header">Live Departures</div>
                <div class="card-body">
                    <p class="card-text">No departure information available</p>
                </div>
            </div>
        </div>

    </div>


    `
}