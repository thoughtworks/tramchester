
import VueTypeaheadBootstrap from 'vue-typeahead-bootstrap'


function sort(stopMap, alreadyDisplayed) {

    if (stopMap==null) {
        return [];
    }

    var stops = Array.from(stopMap.values());
    var results = stops.filter(stop => !alreadyDisplayed.includes(stop.id));
    return results.sort((a,b) => {
        var x = a.name.toLowerCase();
        var y = b.name.toLowerCase();
        return x.localeCompare(y);
    });
}

export default {
    components: {
        VueTypeaheadBootstrap
    },
    /// NOTE: don't camel case these, browser will treat them as all lowercase....
    // TODO Add whether selection is for a dropoff or a pickup
    props: ['value','other','name','bus','stops','geo','disabled'], 
    data: function () {
        return {
            current: this.value
        }
    },
    methods: {
        updateValue(value) {
            this.$emit('input', value);
        },
        serialize: function(station) {
            if (station==null) {
                return "";
            }
            return station.name + ' (' + station.transportModes + ')';
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
            return this.other ? this.other.id : "";
        }
    },
    template: `
    <div>
    <!-- Dropdown selection mode -->
        <b-form-select v-bind:id="name+'Stop'"
                :disabled="disabled"
                :value="value"
                v-on:input="updateValue($event)"
                class="mb-2" required 
                v-if="!bus">
            <option :value="null" disabled>Please select {{name}}</option>
                <optgroup label="Nearby" name="Nearby" :id="name+'GroupNearby'" v-if="geo">
                    <!--<option class="stop" value="MyLocationPlaceholderId">My Location</option>-->
                    <option class="stop" v-for="stop in stops.currentLocation" :value="stop" 
                        :disabled="stop.id == otherId">{{stop.name}}</option>
                </optgroup>
                <optgroup label="Nearest Stops" name="Nearest Stops" :id="name+'GroupNearestStops'" v-if="geo">
                    <option class="stop" v-for="stop in stops.nearestStops" :value="stop" 
                        :disabled="stop.id == otherId">{{stop.name}}</option>
                </optgroup>
                <optgroup label="Recent" name="Recent" :id="name+'GroupRecent'">
                    <option class="stop" v-for="stop in stops.recentStops" :value="stop"
                        :disabled="stop.id == otherId">{{stop.name}}</option>
                </optgroup>
                <optgroup label="All Stops" name="All Stops" :id="name+'GroupAllStops'">
                    <option class="stop" v-for="stop in allstops" :value="stop"
                        :disabled="stop.id == otherId">{{stop.name}}</option>
                </optgroup>
        </b-form-select>
    <!-- Typeahead selection mode -->
        <vue-typeahead-bootstrap
            :disabled="disabled"
            class="mb-4"
            :data="allstops"
            :value="serialize(value)"
            maxMatches=20
            minMatchingChars=3
            :serializer="item => serialize(item)"
            @hit="updateValue($event)"
            placeholder="Select a location"
            v-if="bus"
        />
    </div>
    `
}