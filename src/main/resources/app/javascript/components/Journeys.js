
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
    return station.name;
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
    if (row.actionStation.transportModes.includes('Train')) {
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

function routeFormatter(mode, key, row) {
    if (mode==='Train') {
        return row.route.shortName;
    } else {
        return row.route.routeName;
    }
}

function dateTimeFormatter(value, key, row) {
    var queryDate = row.journey.queryDateAsDate;
    return formatDate(queryDate, value)
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
    var time = toHourAndMins(journeyDateTime); 
    // next day?
    var diff = daysSinceEpoch(journeyDateTime) - daysSinceEpoch(queryDate);
    if (diff>0) {
        return time + ' +' + diff + 'd';
    }
    return time;
}

function actionFormatter(value, key, row) {
    if (row.action=='Walk to' || row.action=='Walk from') {
        return value;
    }
    return value + ' ' + row.route.transportMode;
}

function lineClass(value, key, row) {
    const prefix = 'RouteClass'
    var result = prefix + row.mode;
    if (row.mode=='Tram') {
        result = prefix + row.route.shortName.replace(/\s+/g, '');
    }
    return [ result, 'lineClass'];
}

function earliestDepartTime(journeys) {

    var earliestDepart = null;
    journeys.forEach(item => {
        var currnet = item.journey.firstDepartureTimeAsDate;
        if (earliestDepart==null) {
            earliestDepart = currnet;
        }
        if (currnet < earliestDepart) {
            earliestDepart = currnet;
        }
    })
    return earliestDepart;
}

function getKeyFromChanges(changeStations) {
    var result = "";
    changeStations.forEach(station => result = result + station.id);
    return result;
}

function lastDepartTime(journeys) {

    var lastDepart = null;
    journeys.forEach(item => {
        var currnet = item.journey.firstDepartureTimeAsDate;
        if (lastDepart==null) {
            lastDepart = currnet;
        }
        if (currnet > lastDepart) {
            lastDepart = currnet;
        }
    })
    return lastDepart;
}

function getStageFields() {
    return [{ key: 'firstDepartureTime', label: 'Time', tdClass: 'departTime', formatter: stageDateTimeFormatter },
    { key: 'action', label: 'Action', tdClass: 'action', formatter: actionFormatter },
    { key: 'actionStation.name', label: 'Station', tdClass: 'actionStation', formatter: stationFormatter },
    { key: 'platform.platformNumber', label: 'Platform', tdClass: 'platform' },
    { key: 'headSign', label: 'Headsign', tdClass: stageHeadsignClass },
    { key: 'mode', label: 'Line', formatter: routeFormatter, tdClass: lineClass },
    { key: 'passedStops', label: 'Stops', tdClass: 'passedStops', formatter: stopsFormatter },
    { key: 'expectedArrivalTime', label: 'Arrive', tdClass: 'arriveTime', formatter: stageDateTimeFormatter }];
}


function toHourAndMins(date) {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
}

export default { 
    data: function () {
        return {
            currentPage: 1,
            journeyFields: [
                {key:'_showDetails',label:'', formatter: rowExpandedFormatter},
                {key:'journey.firstDepartureTimeAsDate',label:'Depart', sortable:true, tdClass:'departTime', 
                    formatter: dateTimeFormatter },
                {key:'journey.begin',label:'From', sortable:true, tdClass:'station', formatter: fromFormatter},
                {key:'journey.expectedArrivalTimeAsDate',label:'Arrive', sortable:true, tdClass:'arriveTime'
                    , formatter: dateTimeFormatter},
                {key:'journey.changeStations', label:'Change', tdClass:'changes', formatter: changesFormatter}
                ],
            stageFields: getStageFields(),
            headers: [
                {value: 'data-table-expand', text:'' },
                {value:'journey.firstDepartureTimeAsDate',text:'Depart', sortable:true },
                {value:'journey.begin',text:'From', sortable:true}, 
                {value:'journey.expectedArrivalTimeAsDate',text:'Arrive', sortable:true }, 
                {value:'journey.changeStations', text:'Change' }
                ],
            stageHeaders : [
                { value: 'firstDepartureTime', text: 'Time' }, //tdClass: 'departTime', formatter: stageDateTimeFormatter },
                { value: 'action', text: 'Action' }, //, tdClass: 'action', formatter: actionFormatter },
                { value: 'actionStation.name', text: 'Station' }, //tdClass: 'actionStation', formatter: stationFormatter },
                { value: 'platform.platformNumber', text: 'Platform'}, //, tdClass: 'platform' },
                { value: 'headSign', text: 'Headsign' }, //, tdClass: stageHeadsignClass },
                { value: 'mode', text: 'Line' } , //formatter: routeFormatter, tdClass: lineClass },
                { value: 'passedStops', text: 'Stops' }, //, tdClass: 'passedStops', formatter: stopsFormatter },
                { value: 'expectedArrivalTime', text: 'Arrive' } // , tdClass: 'arriveTime', formatter: stageDateTimeFormatter }
                ],
            sortBy: [{ key: 'firstDepartureTime', order: 'asc' }],
            //singleExpand: true, // for expansion of journey table row
            expanded: []
        }
      },
    props: ['journeysresponse','numjourneystodisplay'],
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
                var changes = getKeyFromChanges(journey.changeStations);
                var key = depart+"_"+arrive+"_"+changes;
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
        },
        itemsPerPage: function() {
            return this.numjourneystodisplay;
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
        }, 
        dateTimeFormatter(value, index) {
            var queryDate = this.journeysresponse[index].journey.queryDateAsDate;
            return formatDate(queryDate, value)
        },
        changesFormatter(value) {
            if (value.length==0) {
                return "Direct";
            }
            var result = "";
            value.forEach(change => {
                if (result.length>0) result = result.concat(", ");
                result = result.concat(change.name)});
            return result;
        }
    },
    template: `
    <div id="journeysComponent">
    <!--
        <b-table id="resultsOld" v-if="journeys.length>0"
                selectable
                sort-icon-left
                :items="journeys" small responsive="sm"
                :fields="journeyFields" 
                :per-page="itemsPerPage"
                :current-page="currentPage" 
                sort-by='journey.expectedArrivalTimeAsDate'
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

        <b-pagination v-if="journeys.length>0 && journeys.length>itemsPerPage"
            v-model="currentPage"
            :total-rows="journeys.length"
            :per-page="itemsPerPage" 
            align="center"
            show-expand
            aria-controls="results">
        </b-pagination>
        -->

        <div id="results" v-if="journeys.length>0">
            <v-data-table id="results"
                :headers="headers"
                :items="journeys"
                item-key="journey.index"
                single-expand=true
                :expanded.sync="expanded"
                show-expand=true
                dense
                v-model:sort-by="sortBy"
                hide-default-footer
                class="elevation-1">
                    <template v-slot:item.journey.firstDepartureTimeAsDate="{ item, index }">
                        <div>{{ dateTimeFormatter(item.journey.firstDepartureTimeAsDate, index) }}</div>
                    </template>
                    <template v-slot:item.journey.expectedArrivalTimeAsDate="{ item, index }">
                        <div>{{ dateTimeFormatter(item.journey.expectedArrivalTimeAsDate, index) }}</div>
                    </template>
                    <template v-slot:item.journey.begin="{ item }">
                        <div>{{ item.journey.begin.name }}</div>
                    </template>
                    <template v-slot:item.journey.changeStations="{ item, index }">
                        <div>{{ changesFormatter(item.journey.changeStations, index) }}</div>
                    </template>
                    <template v-slot:expanded-item="{ headers, item }">
                        <td :colspan="headers.length">
                            Expanded
                        </td>
                    </template>
            </v-data-table>
        </div>

        <div class="container" id="earlierLater" v-if="journeys.length>0">
            <div class="row justify-content-between">
                <div class="col">
                    <button type="button" id="earlierButton" class="btn btn-outline-primary" v-on:click="earlier()">« Earlier</button>
                </div>
                <div class="col">
                    <button type="button" id="laterButton" class="btn btn-outline-primary" v-on:click="later()">Later »</button>
                </div>
            </div>
        </div>

        <div id="noResults" selectable v-if="noJourneys" class="w-75 tramchesterApp">
            <div class="card bg-warning mb-3">
                <div class="card-header">No Results</div>
                <div class="card-body">
                    <p class="card=text">
                        No suggested routes were found for this date and time
                    </p>
                </div>
            </div>
        </div>
    </div>
    `
}


