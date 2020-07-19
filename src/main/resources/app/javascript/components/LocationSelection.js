
import VueBootstrapTypeahead from 'vue-bootstrap-typeahead'

export default {
    components: {
        VueBootstrapTypeahead
    },
    props: ['value','proximitygroups','stops','other','name','bus'], 
    data: function () {
        return {
            current: this.value
        }
    },
    methods: {
        filterStops(group) {
            var result = [];
            var avoidId = this.other
            this.stops.forEach(function(stop) { 
                if (stop.proximityGroup.order===group.order  && stop.id!==avoidId) result.push(stop); 
            } )
            return result.sort((a,b) => a.name.toLowerCase() > b.name.toLowerCase());
        },
        updateValue(value) {
            this.$emit('input', value);
        },
        formName(item) {
            if (item.transportMode=='Tram' || item.transportMode=='Walk') {
                return item.name;
            }
            var name = item.name;
            if (!name.includes(item.area)) {
                name = item.area + " " + name;
            }
            return name + " ("+item.transportMode+")";
        }
    },
    template: `
    <div>
    <!-- trams -->
        <b-form-select v-bind:id="name+'Stop'"
                :value="value"
                v-on:input="updateValue($event)"
                class="mb-2" required
                v-if="!bus">
            <option :value="null" disabled>Please select {{name}}</option>
            <optgroup v-for="group in proximitygroups" :label="group.name" :name="group.name"
                :id="name+'Group'+group.name">
                    <option class="stop" v-for="stop in filterStops(group)" :value="stop.id">{{formName(stop)}}
                </option>
            </optgroup>
        </b-form-select>
    <!-- buses -->
        <vue-bootstrap-typeahead
            class="mb-4"
            :data="stops"
            v-model="current"
            maxMatches=20
            minMatchingChars=3
            :serializer="item => formName(item)"
            @hit="updateValue($event.id)"
            placeholder="Select a location"
            v-if="bus"
        />
    </div>
    `
}