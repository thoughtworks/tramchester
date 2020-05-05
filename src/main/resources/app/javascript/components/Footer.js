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
            //app.networkError = false;
            that.buildnumber = response.data.buildNumber;})
        .catch(function (error) {
            //app.networkError = true;
            console.log(error);
        });
    },
    template: `
    <div id="footer" class="tramchesterFooter mb-1 ml-1 mr-1">
        <b-card bg-variant="light">
            <b-container>
            <b-row>
                <div>
                    Built by <a href="http://www.thoughtworks.com" target="_blank">ThoughtWorks</a> in Manchester
                    <a href="https://twitter.com/intent/user?screen_name=Tramchester" target="_blank">Follow
                        @Tramchester</a>
                </div>
            </b-row>
            <b-row>
                <div id="disclaimer">&copy; 2016-2020 ThoughtWorks Ltd.
                    <b-button id="disclaimerButton"
                              class="align-baseline text-decoration-none tramchesterFooter" variant="link"
                              v-b-modal.modal-disclaimer>Disclaimer
                    </b-button>
                </div>
            </b-row>
           <b-row>
               <div id="license">
                   Contains Transport for Greater Manchester data.
                   Contains public sector information licensed under the Open Government Licence v2.0 by
                   <a href="http://www.datagm.org.uk/" target="_blank">Data GM.</a>
               </div>
           </b-row>
            <b-row align-h="start">
                <b-col cols="3">Build
                    <div id="buildNumber">{{buildnumber}}</div>
                </b-col>
                <b-col cols="3">From:
                    <div id="validFrom">{{feedinfo.validFrom}}</div>
                </b-col>
                <b-col cols="3">Until:
                    <div id="validUntil">{{feedinfo.validUntil}}</div>
                </b-col>
                <b-col cols="3">Geo:
                    <div id="havepos">{{havepos}}</div>
                </b-col>
            </b-row>
            </b-container>
        </b-card>
    </div>
    `
}