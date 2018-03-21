package com.codecool.klondike;

import java.util.ArrayList;
import java.util.List;

public class History {

    ArrayList<Event> events = new ArrayList<Event>();


    public void undo(){
        if(events.size() == 0) return;
        Event lastEvent = events.get(events.size() - 1);
        switch (lastEvent.type){
            case mouseSlide:
                List<Card> cardList = (List<Card>)lastEvent.cards;
                MouseUtil.slideToDest(cardList,lastEvent.previousPile);
                break;
            case moveToDest:
                Card card = (Card)lastEvent.cards;
                card.moveToPile(lastEvent.previousPile);
                card.flip();
                break;
        }
        System.out.println("undo triggered");
        events.remove(lastEvent);
    }

    public void redo(){

    }

    public void addEvent(EventType type, Pile previousPile, Object cards){
        events.add(new Event(type, previousPile, cards));
    }

    private void removeEvent(){

    }
}
