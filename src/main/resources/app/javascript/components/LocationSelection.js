export default {
    props: ['value','proximitygroups','stops','other','name'], 
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
        }
    },
    template: `
    <b-form-select v-bind:id="name+'Stop'"
            :value="value"
            v-on:input="updateValue($event)"
            class="mb-2" required>
        <option :value="null" disabled>Please select {{name}}</option>
        <optgroup v-for="group in proximitygroups" :label="group.name" :name="group.name"
            :id="name+'Group'+group.name">
                <option class="stop" v-for="stop in filterStops(group)" :value="stop.id">{{stop.name}}
            </option>
        </optgroup>
    </b-form-select>
    `
}