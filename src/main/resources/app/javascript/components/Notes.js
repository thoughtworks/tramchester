
export default { 
    props: ['journeysresponse']
    ,
    computed: { 
        notes: function() {
            if (this.journeysresponse==null) {
                return [];
            }
            return this.journeysresponse.notes;
        }
        // ,
        // hasnotes: function() {
        //     if (this.journeysresponse==null) {
        //         return false;
        //     }
        //     return this.journeysresponse.notes.length>0;
        // }
    },
    template: `
    <div id="notesComponent" v-if="notes.length>0">
        <b-card bg-variant="light"
                border-variant="dark" title="Notes" align="center" 
                class="mb-2" >
            <b-card-text>
                <ul id="NotesList" class="JourneyNotes list-group list-group-flush">
                    <li v-for="note in notes" id="NoteItem">
                        <span v-html="note"></span>
                    </li>
                </ul>
            </b-card-text>
        </b-card>
    </div>
    `
}

