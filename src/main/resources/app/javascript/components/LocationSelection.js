
import VueBootstrapTypeahead from 'vue-bootstrap-typeahead'


function sort(values, alreadyDisplayed) {
    var results = values.filter(stop => !alreadyDisplayed.includes(stop.id));
    return results.sort((a,b) => a.name.toLowerCase() > b.name.toLowerCase());
}

export default {
    components: {
        VueBootstrapTypeahead
    },
    /// NOTE: don't camel case these, browser will treat them as all lowercase....
    props: ['value','other','name','bus','stops','geo'], 
    data: function () {
        return {
            current: this.value,
            postcodes: []
        }
    },
    methods: {
        updateValue(value) {
            this.$emit('input', value);
        }
    },
    computed: {
        allstops: function () {
            return sort(this.stops.allStops, this.alreadyDisplayed);
        },
        alreadyDisplayed: function () {
            var results = [];
            if (this.bus) {
                return results; // recent, nearby, etc not used for bus UI
            }

            this.stops.recentStops.forEach(stop => {
                results.push(stop.id)
            });

            this.stops.nearestStops.forEach(stop => {
                results.push(stop.id)
            });

            return results;
        },
        otherId: function() {
            return this.other;
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
            <optgroup label="Nearby" name="Nearby" :id="name+'GroupNearby'" v-if="geo">
                <option class="stop" value="MyLocationPlaceholderId">My Location</option>
            </optgroup>
            <optgroup label="Nearest Stops" name="Nearest Stops" :id="name+'GroupNearestStops'" v-if="geo">
                <option class="stop" v-for="stop in stops.nearestStops" :value="stop.id" 
                :disabled="stop.id == otherId">{{stop.name}}</option>
            </optgroup>
            <optgroup label="Recent" name="Recent" :id="name+'GroupRecent'">
                <option class="stop" v-for="stop in stops.recentStops" :value="stop.id"
                :disabled="stop.id == otherId">{{stop.name}}</option>
            </optgroup>
            <optgroup label="All Stops" name="All Stops" :id="name+'GroupAllStops'">
                <option class="stop" v-for="stop in allstops" :value="stop.id"
                :disabled="stop.id == otherId">{{stop.name}}</option>
            </optgroup>
        </b-form-select>
    <!-- buses -->
        <vue-bootstrap-typeahead
            class="mb-4"
            :data="allstops"
            v-model="current"
            maxMatches=20
            minMatchingChars=3
            :serializer="item => item.name + ' (' + item.transportMode +')' "
            @hit="updateValue($event.id)"
            placeholder="Select a location"
            v-if="bus"
        />
    </div>
    `
}