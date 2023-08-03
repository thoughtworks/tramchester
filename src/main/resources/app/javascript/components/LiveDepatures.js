
function dueTimeFormatter(value, key, row) {
    return value.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
}

export default { 
    props: ['livedataresponse'],
    data: function () {
        return {
            currentPage: 1,
            itemsPerPage: 5,
            departureFields: [
                {key:'transportMode', label:'type', tdClass:'transportMode', sortable: true},
                {key:'from', label:'From', tdClass:'departureDueFrom', sortable:true},
                {key:'dueTimeAsDate', label:'Time', tdClass:'departureDueTime', formatter: dueTimeFormatter, sortable:true},
                {key:'carriages', label:'', tdClass:'departureCarriages'},
                {key:'status', label:'Status', tdClass:'departureStatus'},
                {key:'destination', label:'Towards', tdClass:'departureTowards',  sortable:true}
            ]
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
        }
    },
    template: `
    <div class="container" id="departuesView">
        <b-table id="departures"
            sort-by='dueTimeAsDate'
            sort-icon-left
            v-if="localDueTrams.length>0"
            :items="localDueTrams" 
            small responsive="sm"
            :fields="departureFields" 
            :per-page="itemsPerPage"
            :current-page="currentPage" 
            tbody-tr-class='departuresSummary' caption-top>

            <template v-slot:table-caption>
                <div class="liveDepartures">Current Live Departures</div>
            </template>    
        </b-table>

        <b-pagination v-if="localDueTrams.length>0 && localDueTrams.length>itemsPerPage"
            v-model="currentPage"
            :total-rows="localDueTrams.length"
            :per-page="itemsPerPage" align="center"
            aria-controls="departures"></b-pagination>

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