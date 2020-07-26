
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

function fromFormatter(value, key, row) {
    return nameForStation(value);
}

function nameForStation(station) {
    var name = station.name;
    if (station.id=='MyLocationPlaceholderId') {
        return name;
    }
    if (station.transportMode=='Tram') {
        return name; // tram names unambiguous so no need for area prefx
    }
    if (!name.includes(station.area)) {
        name = station.area + " " + name; // add prefix if not included in name
    }
    return name;
}

function stationFormatter(value, key, row) {
    var name = nameForStation(row.actionStation);
    var url = 'https://www.google.com/maps/search/?api=1&query='+ row.actionStation.latLong.lat + ',' + row.actionStation.latLong.lon;
    return `<a href='${url}' target="_blank">${name}</a>`
}

function stageHeadsignClass(value, key, row) {
    if (row.action=='Walk to' || row.action=='Walk from') {
        return 'walkingHeadSign';
    }
    if (row.actionStation.transportMode=='Train') {
        return 'trainHeadSign';
    }
    return "headsign";
}

function stopsFormatter(value, key, row) {
    if (row.action=='Walk to' || row.action=='Walk from') {
        return '';
    }
    return value;
}

function lineFormatter(value, key, row) {
    if (value==='Bus') {
        return row.routeShortName;
    } else {
        return row.routeName;
    }
}

function lineClass(value, key, item) {
    return item.displayClass;
}

function earliestDepartTime(journeys) {
    var earliestDepart = moment('23:59','HH:mm');
    journeys.forEach(item => {
        var currnet = moment(item.journey.firstDepartureTime,'HH:mm');
        if (currnet.isBefore(earliestDepart)) {
            earliestDepart = currnet;
        }
    })
    return earliestDepart;
}

function lastDepartTime(journeys) {
    var lastDepart = moment('00:01','HH:mm');
    journeys.forEach(item => {
        var currnet = moment(item.journey.firstDepartureTime,'HH:mm');
        if (currnet.isAfter(lastDepart)) {
            lastDepart = currnet;
        }
    })
    return lastDepart;
}

export default { 
    data: function () {
        return {
            journeyFields: [
                {key:'_showDetails',label:'', formatter: rowExpandedFormatter},
                {key:'journey.firstDepartureTime',label:'Depart', sortable:true, tdClass:'departTime'},
                {key:'journey.begin',label:'From', sortable:true, tdClass:'station', formatter: fromFormatter},
                {key:'journey.expectedArrivalTime',label:'Arrive', sortable:true, tdClass:'arriveTime'},
                {key:'journey.changeStations', label:'Change', tdClass:'changes', formatter: changesFormatter}
                ],
            stageFields: [{key:'firstDepartureTime', label:'Time', tdClass:'departTime'},
                {key:'action', label:'Action',tdClass:'action' },
                {key:'actionStation.name', label:'Station', tdClass:'actionStation', formatter: stationFormatter},
                {key:'platform.platformNumber', label:'Platform', tdClass:'platform'},
                {key:'headSign', label:'Headsign', tdClass: stageHeadsignClass },
                {key:'mode', label:'Line', formatter: lineFormatter, tdClass: lineClass },
                {key:'passedStops', label:'Stops', tdClass:'passedStops', formatter: stopsFormatter}]
            }
      },
    props: ['journeysresponse'],
    computed: { 
        journeys: function() {
            if (this.journeysresponse==null) {
                return [];
            }
            var seen = [];
            var result = []
            this.journeysresponse.forEach(item => {
                var journey = item.journey;
                var depart = journey.firstDepartureTime;
                var arrive = journey.expectedArrivalTime;
                var numStages = journey.stages.length;
                var key = depart+"_"+arrive+"_"+numStages;
                if (!seen.includes(key)) {
                    result.push(item);
                    seen.push(key);
                }

            })
            return result;
        },
        noJourneys: function() {
            if (this.journeysresponse==null) {
                return false; // no query has been done
            }
            return this.journeysresponse.length==0;
        }
    },
    methods: {
        expandStages(row,index) {
            row._showDetails = !row._showDetails;
        },
        earlier() {
            const current = earliestDepartTime(this.journeysresponse); 
            var newTime = moment(current,"HH:mm").subtract(24, 'minutes');
            const newDepartTime = newTime.format("HH:mm");
            this.$emit('earlier-tram', newDepartTime);
        },
        later() {
            const current = lastDepartTime(this.journeysresponse); 
            var newTime = moment(current,"HH:mm").add(1, 'minutes');
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
                sort-by='journey.expectedArrivalTime'
                select-mode='single' caption-top
                    @row-clicked="expandStages" tbody-tr-class='journeySummary' caption-top>
            <template v-slot:table-caption>
                <div class="suggestedRoutes">Suggested Routes</div>
            </template>
            <template v-slot:cell(_showDetails)="data">
                <span v-html="data.value">XXX</span>
            </template>
            <template v-slot:row-details="row">
                <b-table :items="row.item.journey.stages" :fields="stageFields"
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