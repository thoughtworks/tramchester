
export default { 
    props: ['notes']
    ,
    template: `
        <b-card bg-variant="light" border-variant="dark" title="Notes" align="center" v-if="notes.length>0"
        class="mb-2">
            <b-card-text>
                <ul id="NotesList" class="JourneyNotes list-group list-group-flush">
                    <li v-for="note in notes" id="NoteItem">
                        <span v-html="note"></span>
                    </li>
                </ul>
            </b-card-text>
        </b-card>
    `
}

