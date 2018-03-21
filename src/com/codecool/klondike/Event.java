package com.codecool.klondike;

import java.util.List;

public class Event {
    public EventType type;
    public Pile previousPile;
    public Object cards;

    Event(EventType type, Pile previousPile, Object cards){
        this.type = type;
        this.previousPile = previousPile;
        this.cards = cards;
    }
}
