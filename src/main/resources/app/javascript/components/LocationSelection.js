
import VueBootstrapTypeahead from 'vue-bootstrap-typeahead'

export default {
    components: {
        VueBootstrapTypeahead
    },
    /// NOTE: don't camel case these, browser will treat them as all lowercase....
    props: ['value','other','name','bus','allstops','recentstops','neareststops','geo'], 
    data: function () {
        return {
            current: this.value
        }
    },
    methods: {
        filterStops(stops) {
            if (stops==null) {
                return [];
            }

            var result = [];
            var avoidId = this.other
            stops.forEach(function(stop) { 
                if (stop.id!==avoidId) result.push(stop); 
            } )
            return result.sort((a,b) => a.name.toLowerCase() > b.name.toLowerCase());
        },
        updateValue(value) {
            this.$emit('input', value);
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
            <optgroup label="Nearby" name="Nearby" :id="name+'Nearby'" v-if="geo">
                <option class="stop" value="MyLocationPlaceholderId">My Location</option>
            </optgroup>
            <optgroup label="Nearest Stops" name="Nearest Stops" :id="name+'Nearest Stops'" v-if="geo">
                <option class="stop" v-for="stop in filterStops(neareststops)" :value="stop.id">{{stop.name}}</option>
            </optgroup>
            <optgroup label="Recent" name="Recent" :id="name+'Recent'">
                <option class="stop" v-for="stop in filterStops(recentstops)" :value="stop.id">{{stop.name}}</option>
            </optgroup>
            <optgroup label="All Stops" name="All Stops" :id="name+'GroupAll Stops'">
                <option class="stop" v-for="stop in filterStops(allstops)" :value="stop.id">{{stop.name}}</option>
            </optgroup>
        </b-form-select>
    <!-- buses -->
        <vue-bootstrap-typeahead
            class="mb-4"
            :data="stops"
            v-model="current"
            maxMatches=20
            minMatchingChars=3
            :serializer="item => item.name"
            @hit="updateValue($event.id)"
            placeholder="Select a location"
            v-if="bus"
        />
    </div>
    `
}