
function consolidateNotes(journeys) {
    var notes = [];
    var notesText = [];
    journeys.forEach(item => {
        var journey = item.journey;
        var journeyNotes = journey.notes;
        journeyNotes.forEach(journeyNote => {
            var text = journeyNote.text;
            if (!notesText.includes(text)) {
                notes.push(journeyNote);
                notesText.push(text);
            }
        })
    });
    return notes;
}

function countItems(text, list) {
    var count = 0;
    list.forEach(item => {
        if (text==item) {
            count++;
        }
    });
    return count;
}

export default { 
    props: ['journeys','livedataresponse']
    ,
    computed: { 
        notes: function() {

            var allNotes = [];
            if (this.journeys!=null) {
                this.journeys.forEach(item => {
                    item.journey.notes.forEach(note => {
                        allNotes.push(note);
                    })
                });    
            } else if (this.livedataresponse!=null) {
                this.livedataresponse.notes.forEach(note => {
                    allNotes.push(note);
                });   
            }
            
            return allNotes;
        },
        liveMessages: function() {
            var messages = [];

            var allContents = [];
            var produced = [];
            this.notes.forEach(note => {
                allContents.push(note.text);
            });
            this.notes.forEach(note => {
                var result = note.text;
                if (note.noteType=='Live') {
                    var count = countItems(note.text, allContents);
                    result = "'" + note.text + "' - Metrolink";
                    if (count==1) {
                        result = result + ", " + note.stationRef.name;
                    } 
                }
                if (!produced.includes(result)) {
                    messages.push({noteType: note.noteType, text: result}); 
                    produced.push(result);
                } 

            });
            return messages;
        }
    },
    template: `
    <div id="notesComponent" v-if="notes.length>0">
        <b-card bg-variant="light"
                border-variant="dark" title="Notes" align="center" 
                class="mb-2" >
            <b-card-text>
                <ul id="NotesList" class="JourneyNotes list-group list-group-flush">
                    <li v-for="note in liveMessages" id="NoteItem">
                        <span :id="note.noteType" v-html="note.text"></span>
                    </li>
                </ul>
            </b-card-text>
        </b-card>
    </div>
    `
}

