package com.tramchester.domain.presentation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Objects;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = StationNote.class)})
public class Note {
    private NoteType noteType;
    private String text;

    public Note(String text, NoteType noteType) {
        this.text = text;
        this.noteType = noteType;
    }

    public Note() {
        // deserialisation
    }

    public NoteType getNoteType() {
        return noteType;
    }

    public String getText() {
        return text;
    }

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
                Objects.equals(text, note.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(noteType, text);
    }

    @Override
    public String toString() {
        return "Note{" +
                "noteType=" + noteType +
                ", text='" + text + '\'' +
                '}';
    }
}
