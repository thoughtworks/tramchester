package com.tramchester.domain.presentation;

public enum TravelAction {
    Leave, Change, Board, WalkTo {
        @Override
        public String toString() {
            return "Walk to";
        }
    }, WalkFrom {
        @Override
        public String toString() {
            return "Walk from";
        }
    }, ConnectTo {
        @Override
        public String toString() { return "Walk between"; }
    }

}
