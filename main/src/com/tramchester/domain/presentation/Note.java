package com.tramchester.domain.presentation;

import java.util.Objects;

public class Note {

    public enum NoteType {
        Weekend,
        Christmas,
        ClosedStation,
        Live
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        return noteType == note.noteType &&
                text.equals(note.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(noteType, text);
    }

    private NoteType noteType;
    private String text;

    public Note() {
        // deserialisation
    }

    public Note(NoteType noteType, String text) {
        this.noteType = noteType;
        this.text = text;
    }

    @Override
    public String toString() {
        return "Note{" +
                "noteType=" + noteType +
                ", text='" + text + '\'' +
                '}';
    }

    public NoteType getNoteType() {
        return noteType;
    }

    public String getText() {
        return text;
    }
}
