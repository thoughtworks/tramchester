
var moment = require('moment');

function dueTimeFormatter(value, key, row) {
    var departTime = moment(value);
    return departTime.format("HH:mm");
}

export default { 
    props: ['livedataresponse'],
    data: function () {
        return {
            currentPage: 1,
            departureFields: [
                {key:'from', label:'From', tdClass:'departureDueFrom', sortable:true},
                {key:'dueTime', label:'Time', tdClass:'departureDueTime', formatter: dueTimeFormatter, sortable:true},
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
    <div id="departuesView">
        <b-table id="departures"
            v-if="localDueTrams.length>0"
            :current-page="currentPage" sort-icon-left
            :items="localDueTrams" small responsive="sm"
            :fields="departureFields" per-page="4"
            tbody-tr-class='departuresSummary' caption-top>
            <template v-slot:table-caption>
                <div class="liveDepartures">Current Live Departures</div>
            </template>
        </b-table>
        <b-pagination v-if="localDueTrams.length>0 && localDueTrams.length>4"
            v-model="currentPage"
            :total-rows="localDueTrams.length"
            per-page="4" align="center"
            aria-controls="departures"></b-pagination>

        <div id="noLiveResults" selectable v-if="noLiveResults" class="w-75 tramchesterApp">
            <b-card bg-variant="light">
                No real time departure information found for query time and date
            </b-card>
        </div>
    </div>


    `
}