

export default { 
    props: ['livedataresponse'],
    data: function () {
        return {
            itemsPerPage: 5,
            page: 1,
            headers: [
                {value:'transportMode', text:'type', sortable: true},
                {value:'from', text:'From', sortable:true},
                {value:'dueTimeAsDate', text:'Time', sortable: true}, 
                {value:'carriages', text:''},
                {value:'status', text:'Status'},
                {value:'destination', text:'Towards', sortable:true}
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
         pageCount () {
             return Math.ceil(this.localDueTrams.length / this.itemsPerPage)
           },
    },
    methods: {
        dueTimeFormatter(value) {
            return value.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
        }
    },
    template: `
    <div class="container" id="departuesView">

        <div id="departuresTable" v-if="localDueTrams.length>0">
            <v-data-table id="departures"
                :headers="headers" 
                :items="localDueTrams"
                dense
                v-model:sort-by="sortBy"
                :items-per-page="itemsPerPage"
                hide-default-footer
                class="elevation-1">
                <template v-slot:item.dueTimeAsDate="{ item }">
                    <div>{{ dueTimeFormatter(item.dueTimeAsDate) }}</div>
                </template>
            </v-data-table>
        </div>
        <div class="text-center pt-2" v-if="localDueTrams.length>0">
                <v-pagination
                  v-model="page"
                  :length="pageCount"
                ></v-pagination>

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