package com.codecool.klondike;

import com.sun.xml.internal.bind.v2.TODO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Game extends Pane {

    private List<Card> deck = new ArrayList<>();

    private Pile stockPile;
    private Pile discardPile;
    private List<Pile> foundationPiles = FXCollections.observableArrayList();
    private List<Pile> tableauPiles = FXCollections.observableArrayList();

    private double dragStartX, dragStartY;
    public List<Card> draggedCards = FXCollections.observableArrayList();

    private static double STOCK_GAP = 1;
    private static double FOUNDATION_GAP = 0;
    private static double TABLEAU_GAP = 30;
    public static History history;


    private EventHandler<MouseEvent> onMouseClickedHandler = e -> {
        Card card = (Card) e.getSource();
        if (card.getContainingPile().getPileType() == Pile.PileType.STOCK) {
            e.consume();
            history.addEvent(EventType.moveToDiscard, card.getContainingPile(), card);
            card.moveToPile(discardPile);
            card.flip();
            card.setMouseTransparent(false);
            System.out.println("Placed " + card + " to the waste.");
        }
        if ((card.getContainingPile().getPileType() == Pile.PileType.TABLEAU ||
                card.getContainingPile().getPileType() == Pile.PileType.DISCARD) &&
                card.equals(card.getContainingPile().getTopCard()) &&
                !card.isFaceDown() &&
                e.getClickCount() == 2 && !e.isConsumed()) {
            e.consume();
            handleDoubleClick(card);
        }
    };

    /**
     * If double-clicked on a faceup card in the Discard pile or the Tableau piles:
     * Checks if the card can be placed to one of the foundation piles.
     * If the move is valid, the card is placed to that foundation pile.
     *
     * @param card the card that was clicked twice.
     */
    private void handleDoubleClick(Card card) {
        for (Pile pile : foundationPiles) {
            if (pile.isEmpty()) {
                if (card.getRank() == Card.Rank.ACE) {
                    Pile containingPile = card.getContainingPile();
                    history.addEvent(EventType.moveToFoundation, containingPile, card);
                    card.moveToPile(pile);
                    Pile.flipTopCardOfTableau(containingPile);
                    break;
                }
            } else if (pile.getTopCard().getSuit() == card.getSuit() &&
                    pile.getTopCard().getRank().ordinal() + 1 == card.getRank().ordinal()) {
                Pile containingPile = card.getContainingPile();
                history.addEvent(EventType.moveToFoundation, containingPile, card);
                card.moveToPile(pile);
                Pile.flipTopCardOfTableau(containingPile);
                break;
            }
        }
    }

    private EventHandler<MouseEvent> stockReverseCardsHandler = e -> {
        refillStockFromDiscard();
    };

    private EventHandler<MouseEvent> onMousePressedHandler = e -> {
        if (!draggedCards.isEmpty()) return;
        ;
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
    };
    private EventHandler<MouseEvent> onMouseDraggedHandler = e -> {
        if (draggedCards.isEmpty()) {
            Card card = (Card) e.getSource();
            Pile activePile = card.getContainingPile();
            List<Card> cardsToDrag = activePile.getCardAndbelow(activePile.getCards().indexOf(card));
            if (activePile.getPileType() == Pile.PileType.STOCK) {
                return;
            }
            draggedCards = cardsToDrag;
        }
        double offsetX = e.getSceneX() - dragStartX;
        double offsetY = e.getSceneY() - dragStartY;


        for (int i = 0; i < draggedCards.size(); i++) {
            Card current = draggedCards.get(i);
            current.getDropShadow().setRadius(20);
            current.getDropShadow().setOffsetX(10);
            current.getDropShadow().setOffsetY(10);

            current.toFront();
            current.setTranslateX(offsetX);
            current.setTranslateY(offsetY + (i * 10));
        }


    };

    private EventHandler<MouseEvent> onMouseReleasedHandler = e -> {
        if (draggedCards.isEmpty()) {
            return;
        }

        Card card = (Card) e.getSource();
        Pile pile = getValidIntersectingPile(card, foundationPiles);
        if (pile == null) {
            pile = getValidIntersectingPile(card, tableauPiles);
        }
        if (pile != null) {
            handleValidMove(card, pile);
        } else {
            draggedCards.forEach(MouseUtil::slideBack);
        }
    };

    public boolean isGameWon() {
        //TODO
        return false;
    }

    private void addButtons() {
        Button unDoButton = new Button("Undo");
        unDoButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                history.undo();
            }
        });
        this.getChildren().add(unDoButton);
    }

    public Game(Stage primaryStage) {
        deck = Card.createNewDeck();
        initPiles();
        dealCards();
        addButtons();
        setRestartButton(primaryStage);
        getChildren().add(setRestartButton(primaryStage));
        history = new History();
    }

    public void addMouseEventHandlers(Card card) {
        card.setOnMousePressed(onMousePressedHandler);
        card.setOnMouseDragged(onMouseDraggedHandler);
        card.setOnMouseReleased(onMouseReleasedHandler);
        card.setOnMouseClicked(onMouseClickedHandler);
    }

    public void refillStockFromDiscard() {
        //TODO
        System.out.println("Stock refilled from discard pile.");
    }

    public boolean isMoveValid(Card card, Pile destPile) {
        if (destPile.getPileType() == Pile.PileType.FOUNDATION) {
            return isMoveToFoundationValid(card, destPile);
        } else if (destPile.getPileType().equals(Pile.PileType.TABLEAU)) {
            return isMoveToTableauValid(card, destPile);
        } else {
            return false;
        }
    }

    /**
     * Checks the move of a card to a tableau pile is valid:
     * Empty pile and the card is King.
     * Not empty pile, and the value of the card is one less than the destination pile based on Rank.
     * Not empty pile, and the color of the card is the opposite than the top card on the destination pile based on the Suit.
     * It returns 'true' if the move is valid, and 'false' if the move is invalid.
     *
     * @param card     the card that has to pass the check (one that is being moved, or bottom one of a moving set of cards).
     * @param destPile the pile that the moved card would be placed on (in case this test passes)
     * @return true if the move is valid, false if the move is invalid.
     */
    private boolean isMoveToTableauValid(Card card, Pile destPile) {
        int draggedCardValue = card.getRank().ordinal();
        if (destPile.isEmpty()) {
            if (card.getRank().name().equals("KING")) {
                return true;
            } else {
                return false;
            }
        } else if (Card.isOppositeColor(card, destPile.getTopCard()) && destPile.getTopCardValue() == draggedCardValue + 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks the move of a card to a foundation pile is valid:
     * Empty pile and the card is Ace.
     * Not empty pile, and the card is the next in order, based on Rank.
     * It returns 'true' if the move is valid, and 'false' if the move is invalid.
     *
     * @param card     the card that has to pass the check (one that is being moved, or bottom one of a moving set of cards).
     * @param destPile the pile that the moved card would be placed on (in case this test passes)
     * @return true if the move is valid, false if the move is invalid.
     */
    private boolean isMoveToFoundationValid(Card card, Pile destPile) {
        if (destPile.isEmpty()) {
            if (card.getRank() == Card.Rank.ACE) {
                return true;
            } else {
                return false;
            }
        } else if (destPile.getTopCard().getRank().ordinal() + 1 == card.getRank().ordinal() &&
                destPile.getTopCard().getSuit() == card.getSuit()) {
            return true;
        } else {
            return false;
        }
    }

    private Pile getValidIntersectingPile(Card card, List<Pile> piles) {
        Pile result = null;
        for (Pile pile : piles) {
            if (!pile.equals(card.getContainingPile()) &&
                    isOverPile(card, pile) &&
                    isMoveValid(card, pile))
                result = pile;
        }
        return result;
    }

    private boolean isOverPile(Card card, Pile pile) {
        if (pile.isEmpty())
            return card.getBoundsInParent().intersects(pile.getBoundsInParent());
        else
            return card.getBoundsInParent().intersects(pile.getTopCard().getBoundsInParent());
    }

    private void handleValidMove(Card card, Pile destPile) {
        String msg = null;
        if (destPile.isEmpty()) {
            if (destPile.getPileType().equals(Pile.PileType.FOUNDATION))
                msg = String.format("Placed %s to the foundation.", card);
            if (destPile.getPileType().equals(Pile.PileType.TABLEAU))
                msg = String.format("Placed %s to a new pile.", card);
        } else {
            msg = String.format("Placed %s to %s.", card, destPile.getTopCard());
        }
        System.out.println(msg);
        MouseUtil.slideToDest(draggedCards, destPile);
        history.addEvent(EventType.mouseSlide, draggedCards.get(0).getContainingPile(), FXCollections.observableArrayList(draggedCards));
    }


    private void initPiles() {
        stockPile = new Pile(Pile.PileType.STOCK, "Stock", STOCK_GAP);
        stockPile.setBlurredBackground();
        stockPile.setLayoutX(95);
        stockPile.setLayoutY(20);
        stockPile.setOnMouseClicked(stockReverseCardsHandler);
        getChildren().add(stockPile);

        discardPile = new Pile(Pile.PileType.DISCARD, "Discard", STOCK_GAP);
        discardPile.setBlurredBackground();
        discardPile.setLayoutX(285);
        discardPile.setLayoutY(20);
        getChildren().add(discardPile);

        for (int i = 0; i < 4; i++) {
            Pile foundationPile = new Pile(Pile.PileType.FOUNDATION, "Foundation " + i, FOUNDATION_GAP);
            foundationPile.setBlurredBackground();
            foundationPile.setLayoutX(610 + i * 180);
            foundationPile.setLayoutY(20);
            foundationPiles.add(foundationPile);
            getChildren().add(foundationPile);
        }
        for (int i = 0; i < 7; i++) {
            Pile tableauPile = new Pile(Pile.PileType.TABLEAU, "Tableau " + i, TABLEAU_GAP);
            tableauPile.setBlurredBackground();
            tableauPile.setLayoutX(95 + i * 180);
            tableauPile.setLayoutY(275);
            tableauPiles.add(tableauPile);
            getChildren().add(tableauPile);
        }
    }

    /**
     * Deals the cards to the tableau columns at the start of the game.
     * Each column gets one more cards then the first one.
     * The top cards of the stockPile gets flipped the others are turned over.
     */
    public void dealCards() {
        Iterator<Card> deckIterator = deck.iterator();
        deckIterator.forEachRemaining(card -> {
            stockPile.addCard(card);
            addMouseEventHandlers(card);
            getChildren().add(card);
        });
        for (int tableauColumn = 0; tableauColumn < tableauPiles.size(); tableauColumn++) {
            Pile currentPile = tableauPiles.get(tableauColumn);

            for (int j = 0; j < tableauColumn; j++) {
                stockPile.getTopCard().moveToPile(currentPile);
            }
            Card topCard = stockPile.getTopCard();
            topCard.flip();
            topCard.moveToPile(currentPile);
        }

    }


    public void setTableBackground(Image tableBackground) {
        setBackground(new Background(new BackgroundImage(tableBackground,
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
    }

    private void autoWinGame() {
        List<Pile> currentPiles = FXCollections.observableArrayList();
        currentPiles.add(discardPile);
        currentPiles.addAll(tableauPiles);
        while (cardsOnTable()) {
            Card smallest = null;
            for (Pile pile : currentPiles) {
                Card temp = pile.getTopCard();
                if (temp != null) {
                    if (smallest == null || smallest.getRank().ordinal() < temp.getRank().ordinal()) {
                        smallest = temp;
                    }
                }
            }
            List<Card> temp = new ArrayList<>(1);
            temp.add(smallest);
            MouseUtil.slideToDest(temp, autoSelectDest(smallest));
        }

    }

    private Pile autoSelectDest(Card card) {
        for (Pile pile : foundationPiles) {
            if (card.getRank() == Card.Rank.ACE) {
                if (pile.isEmpty()) {
                    return pile;
                } else {
                    throw new RuntimeException("ERROR: No empty foundation pile found for Ace.");
                }
            } else {
                if (card.getSuit() == pile.getTopCard().getSuit()) {
                    if (card.getRank().ordinal() - 1 == pile.getTopCard().getRank().ordinal()) {
                        return pile;
                    } else {
                        throw new RuntimeException("ERROR: Card can't be placed in matching suit foundation pile. ");
                    }
                }
            }
        }
        throw new RuntimeException("ERROR: Failed automatic destination selection.");
    }

    /**
     * Checks if there are still cards in any of the Stock, Discard or Foundation piles.
     *
     * @return true if there are cards on the tabe, false otherwise
     */
    private boolean cardsOnTable() {
        for (Pile pile : foundationPiles) {
            if (!pile.isEmpty()) {
                return false;
            }
        }
        if (!(discardPile.isEmpty() && stockPile.isEmpty())) {
            return false;
        }
        return true;
    }

    /**
     * CHecks if all cards on the board (discard, stock and tableau Piles) are face up.
     *
     * @return true if there are no facedown cards, false otherwise
     */
    private boolean allCardsFaceUp() {
        List<Pile> currentPiles = FXCollections.observableArrayList();
        currentPiles.add(discardPile);
        currentPiles.add(stockPile);
        currentPiles.addAll(tableauPiles);
        for (Pile pile : currentPiles) {
            for (Card card : pile.getCards()) {
                if (card.isFaceDown()) {
                    return false;
                }
            }
        }
        return true;
    }

    public Button setRestartButton(Stage primaryStage) {
        Button restartButton = new Button();
        restartButton = formatRestartButton(restartButton);
        restartButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Klondike.startGame(primaryStage);
            }
        });
        return restartButton;
    }

    private Button formatRestartButton(Button restartButton) {
        restartButton.setText("Restart");
        restartButton.setPrefWidth(70);
        restartButton.setPrefHeight(30);
        restartButton.setLayoutX(0);
        restartButton.setLayoutY(50);
        return restartButton;
    }

}
