
var moment = require('moment');

function rowExpandedFormatter(value, key, row) {
    if (row._showDetails!=null && row._showDetails) {
        return "&#8897;";
    } else {
        return "&#8811;";
    }
}

function changesFormatter(value, key, row) {
    if (value.length==0) {
        return "Direct";
    }
    var result = "";
    value.forEach(change => {
        if (result.length>0) result = result.concat(", ");
        result = result.concat(change)});
    return result;
}


function stationFormatter(value, key, row) {
    var url = 'https://www.google.com/maps/search/?api=1&query='
        + row.actionStation.latLong.lat + ',' + row.actionStation.latLong.lon;
    return `<a href='${url}' target="_blank">${row.actionStation.name}</a>`
}

function stageHeadsignClass(value, key, item) {
    if (value === 'WalkingHeadSign') {
        return 'HideWalkingHeadSign';
    }
    return "headsign";
}

function lineFormatter(value, key, row) {
    if (value==='Bus') {
        return row.routeShortName;
    } else {
        return row.routeName;
    }
}

function stageRowClass(value, key, item) {
    return item.displayClass;
}

export default { 
    data: function () {
        return {
            journeyFields: [
                {key:'_showDetails',label:'', formatter: rowExpandedFormatter},
                {key:'firstDepartureTime',label:'Depart', sortable:true, tdClass:'departTime'},
                {key:'begin.name',label:'From', sortable:true, tdClass:'station'},
                {key:'expectedArrivalTime',label:'Arrive', sortable:false, tdClass:'arriveTime'},
                {key:'changeStations', label:'Change', tdClass:'changes', formatter: changesFormatter}
                ],
            stageFields: [{key:'firstDepartureTime',label:'Time',tdClass:'departTime'},
                {key:'action', label:'Action',tdClass:'action' },
                {key:'actionStation.name', label:'Station', tdClass:'actionStation', formatter: stationFormatter},
                {key:'platform.platformNumber', label:'Platform', tdClass:'platform'},
                {key:'headSign', label:'Towards', tdClass: stageHeadsignClass },
                {key:'mode', label:'Line', formatter: lineFormatter, tdClass: stageRowClass },
                {key:'passedStops', label:'Stops', tdClass:'passedStops'}]
            }
      },
    props: ['journeysresponse', 'querytime'],
    computed: { 
        journeys: function() {
            if (this.journeysresponse==null) {
                return [];
            }
            return this.journeysresponse.journeys;
        },
        noJourneys: function() {
            if (this.journeysresponse==null) {
                return false;
            }
            return this.journeysresponse.journeys.length==0;
        }
    },
    methods: {
        expandStages(row,index) {
            row._showDetails = !row._showDetails;
        },
        earlier() {
            const firstJourney = this.journeysresponse.journeys[0];
            const currentEarliest = firstJourney.firstDepartureTime;
            var newTime = moment(currentEarliest,"HH:mm").subtract(24, 'minutes');
            const newDepartTime = newTime.format("HH:mm");
            this.$emit('earlier-tram', newDepartTime);
        },
        later() {
            const indexOfLast = this.journeysresponse.journeys.length - 1;
            const lastJourney = this.journeysresponse.journeys[indexOfLast];
            const currentLatest = lastJourney.firstDepartureTime;
            var newTime = moment(currentLatest,"HH:mm").add(1, 'minutes');
            const newDepartTime = newTime.format("HH:mm");
            this.$emit('later-tram', newDepartTime);
        }
    },
    template: `
    <div id="journeysComponent">
        <b-table id="results"  v-if="journeys.length>0"
                selectable
                sort-icon-left
                :items="journeys" small responsive="sm"
                :fields="journeyFields"
                select-mode='single' caption-top
                    @row-clicked="expandStages" tbody-tr-class='journeySummary' caption-top>
            <template v-slot:table-caption>
                <div class="suggestedRoutes">Suggested Routes</div>
            </template>
            <template v-slot:cell(_showDetails)="data">
                <span v-html="data.value">XXX</span>
            </template>
            <template v-slot:row-details="row">
                <b-table :items="row.item.stages" :fields="stageFields"
                id="stages" tbody-tr-class='stageSummary' small outlined>
                    <template v-slot:cell(actionStation.name)="data">
                        <span v-html="data.value"></span>
                    </template>
                </b-table>
            </template>
        </b-table>
        <div class="container" id="earlierLater" v-if="journeys.length>0">
            <div class="row justify-content-between">
                <b-button id="earlierButton" variant="outline-primary" v-on:click="earlier()">« Earlier</b-button>
                <b-button id="laterButton" variant="outline-primary" v-on:click="later()">Later »</b-button>
            </div>
        </div>
        <div id="noResults" selectable v-if="noJourneys" class="w-75 tramchesterApp">
            <b-card bg-variant="warning">
                No suggested routes were found for this date and time
            </b-card>
        </div>
    </div>
    `
}