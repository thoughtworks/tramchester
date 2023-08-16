

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
    <div class="container" id="notesComponent" v-if="notes.length>0">
        <div class="card mb-2 border-dark bg-light" align="center">
            <div class="card-body">
                <h4 class="card-title">Notes</h4>
                <ul id="NotesList" class="card-text JourneyNotes list-group list-group-flush">
                    <li v-for="note in liveMessages" id="NoteItem">
                        <span :id="note.noteType" v-html="note.text"></span>
                    </li>
                </ul>
            </div>
        </div>
    </div>
    `
}

