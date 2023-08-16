const axios = require('axios');

export default { 
    data: function() {
        return {
            buildnumber: []
        }
    },
    props: ['havepos','feedinfo'],
    mounted() {
        var that = this
        axios.get('/api/version')
        .then(function (response) {
            that.buildnumber = response.data.buildNumber;})
        .catch(function (error) {
            this.$emit('network-error')
            console.log(error)
        });
    },
    template: `
    <div id="footer" class="tramchesterFooter">
        <div class="card mt-1">
            <div class="container mt-3">
                <div class="row">
                    <div>
                        Built by <a href="http://www.thoughtworks.com" target="_blank">ThoughtWorks</a> in Manchester
                    </div>
                </div>
                <div class="row">
                    <div id="disclaimer">
                        &copy; 2016-2020 ThoughtWorks Ltd.
                        <button id="disclaimerButton" data-toggle="modal" data-target="#modaldisclaimer"
                                  class="btn btn-link align-baseline text-decoration-none tramchesterFooter">
                                  Disclaimer
                        </button>
                    </div>
                </div>
               <div class="row">
                   <div id="license">
                       Contains Transport for Greater Manchester data.
                       Contains public sector information licensed under the Open Government Licence v2.0 by
                       <a href="http://www.datagm.org.uk/" target="_blank">Data GM.</a>
                       Contains public sector information licensed under the Open Government Licence v3.0.
                       Timetable/Fares/London Terminals data under licence from <a href="http://www.raildeliverygroup.com/">RSP</a>.
                   </div>
               </div>
                <div class="row mt-1">
                    <div class="col">Build
                        <div id="buildNumber">{{buildnumber}}</div>
                    </div>
                    <div class="col">Data Version:
                        <div id="dataVersion">{{feedinfo.version}}</div>
                    </div>
                    <div class="col">Geo:
                        <div id="havepos">{{havepos}}</div>
                    </div>
                </div>
            </div>
        <div>
    </div>
    `
}