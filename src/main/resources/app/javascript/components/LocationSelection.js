
import VueBootstrapTypeahead from 'vue-bootstrap-typeahead'


function sort(values,alreadyDisplayed) {
    var results = values.filter(stop => !alreadyDisplayed.includes(stop.id));
    return results.sort((a,b) => a.name.toLowerCase() > b.name.toLowerCase());
}

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
        updateValue(value) {
            this.$emit('input', value);
        }
    },
    computed: {
        stops: function () {
            return sort(this.allstops, this.alreadyDisplayed);
        },
        alreadyDisplayed: function () {
            var results = [];
            if (this.bus) {
                return results;
            }

            this.recentstops.forEach(stop => {
                results.push(stop.id)
            });
            this.neareststops.forEach(stop => {
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
                <option class="stop" v-for="stop in neareststops" :value="stop.id" 
                :disabled="stop.id == otherId">{{stop.name}}</option>
            </optgroup>
            <optgroup label="Recent" name="Recent" :id="name+'GroupRecent'">
                <option class="stop" v-for="stop in recentstops" :value="stop.id"
                :disabled="stop.id == otherId">{{stop.name}}</option>
            </optgroup>
            <optgroup label="All Stops" name="All Stops" :id="name+'GroupAllStops'">
                <option class="stop" v-for="stop in stops" :value="stop.id"
                :disabled="stop.id == otherId">{{stop.name}}</option>
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