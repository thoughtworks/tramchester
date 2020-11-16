
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
        result = result.concat(change.name)});
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

function dateTimeFormatter(value, key, row) {
    var queryDate = new Date(row.journey.queryDate);
    var journeyDate = new Date(value);
    return formatDate(queryDate, journeyDate)
}

function stageDateTimeFormatter(value, key, row) {
    var queryDate = new Date(row.queryDate);
    var journeyDate = new Date(value);
    return formatDate(queryDate, journeyDate)
}

function daysSinceEpoch(date) {
    const dayInMillis = Math.floor(24 * 60 * 60 * 1000);
    return Math.floor(date.getTime() / dayInMillis);
}

function formatDate(queryDate, journeyDateTime) {
    var diff = daysSinceEpoch(journeyDateTime) - daysSinceEpoch(queryDate);
    var time = toHourAndMins(journeyDateTime); 
    if (diff>0) {
        return time + ' +' + diff + 'd';
    }
    return time;
}

function actionFormatter(value, key, row) {
    if (row.action=='Walk to' || row.action=='Walk from') {
        return value;
    }
    return value + ' ' + row.actionStation.transportMode;
}

function lineClass(value, key, item) {
    const prefix = 'RouteClass'
    if (item.mode=='Tram') {
        return prefix + item.routeShortName;
    }
    return prefix + item.mode;
}

function earliestDepartTime(journeys) {

    var earliestDepart = null;
    journeys.forEach(item => {
        var currnet = new Date(item.journey.firstDepartureTime);
        if (earliestDepart==null) {
            earliestDepart = currnet;
        }
        if (currnet < earliestDepart) {
            earliestDepart = currnet;
        }
    })
    return earliestDepart;
}

function lastDepartTime(journeys) {

    var lastDepart = null;
    journeys.forEach(item => {
        var currnet = new Date(item.journey.firstDepartureTime);
        if (lastDepart==null) {
            lastDepart = currnet;
        }
        if (currnet > lastDepart) {
            lastDepart = currnet;
        }
    })
    return lastDepart;
}


function toHourAndMins(date) {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
}

export default { 
    data: function () {
        return {
            journeyFields: [
                {key:'_showDetails',label:'', formatter: rowExpandedFormatter},
                {key:'journey.firstDepartureTime',label:'Depart', sortable:true, tdClass:'departTime', 
                    formatter: dateTimeFormatter },
                {key:'journey.begin',label:'From', sortable:true, tdClass:'station', formatter: fromFormatter},
                {key:'journey.expectedArrivalTime',label:'Arrive', sortable:true, tdClass:'arriveTime'
                    , formatter: dateTimeFormatter},
                {key:'journey.changeStations', label:'Change', tdClass:'changes', formatter: changesFormatter}
                ],
            stageFields: [{key:'firstDepartureTime', label:'Time', tdClass:'departTime', formatter: stageDateTimeFormatter},
                {key:'action', label:'Action', tdClass:'action', formatter: actionFormatter },
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
            var newTime = new Date(current.getTime() - 24*60*1000); 
            const newDepartTime = toHourAndMins(newTime); 
            this.$emit('earlier-tram', newDepartTime);
        },
        later() {
            const current = lastDepartTime(this.journeysresponse); 
            var newTime = new Date(current.getTime() + 60*1000); 
            const newDepartTime = toHourAndMins(newTime); 
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