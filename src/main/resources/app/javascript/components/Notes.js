
export default { 
    props: ['journeysresponse','livedataresponse']
    ,
    computed: { 
        notes: function() {
            if (this.journeysresponse!=null) {
                if (this.journeysresponse.notes.length>0) {
                    return this.journeysresponse.notes;
                }
            }

            if (this.livedataresponse!=null) {
                return this.livedataresponse.notes;
            }
            return [];
            
        }
    },
    template: `
    <div id="notesComponent" v-if="notes.length>0">
        <b-card bg-variant="light"
                border-variant="dark" title="Notes" align="center" 
                class="mb-2" >
            <b-card-text>
                <ul id="NotesList" class="JourneyNotes list-group list-group-flush">
                    <li v-for="note in notes" id="NoteItem">
                        <span :id="note.noteType" v-html="note.text"></span>
                    </li>
                </ul>
            </b-card-text>
        </b-card>
    </div>
    `
}

